import { useState } from 'react'

type Result = 'PASS' | 'WARN' | 'FAIL'

interface Check {
  id: string
  label: string
  result: Result
  detail: string
}

interface Report {
  url: string
  finalUrl: string
  status: number
  grade: string
  checks: Check[]
  wordpress: { detected: boolean; version: string | null; findings: string[] }
}

export default function App() {
  const [url, setUrl] = useState('')
  const [report, setReport] = useState<Report | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function scan(e: React.FormEvent) {
    e.preventDefault()
    const target = url.includes('://') ? url.trim() : `https://${url.trim()}`
    setLoading(true)
    setError('')
    setReport(null)
    try {
      const res = await fetch(`/api/scan?url=${encodeURIComponent(target)}`)
      const body = await res.json().catch(() => ({}))
      if (!res.ok) throw new Error(body.error ?? `request failed (${res.status})`)
      setReport(body)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'request failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main>
      <h1>headers-check</h1>
      <form onSubmit={scan}>
        <input
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="https://example.com"
          aria-label="URL to scan"
        />
        <button disabled={loading || !url.trim()}>{loading ? 'Scanning...' : 'Scan'}</button>
      </form>

      {error && <p className="error">{error}</p>}

      {report && (
        <>
          <section className="summary">
            <div className={`grade grade-${report.grade}`}>{report.grade}</div>
            <div>
              <div>{report.finalUrl}</div>
              <div className="muted">HTTP {report.status}</div>
            </div>
          </section>

          <table>
            <thead>
              <tr>
                <th>Check</th>
                <th>Result</th>
                <th>Detail</th>
              </tr>
            </thead>
            <tbody>
              {report.checks.map((c) => (
                <tr key={c.id}>
                  <td>{c.label}</td>
                  <td>
                    <span className={`tag ${c.result}`}>{c.result}</span>
                  </td>
                  <td>{c.detail}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {report.wordpress.detected && (
            <section className="wp">
              <h2>WordPress{report.wordpress.version && ` ${report.wordpress.version}`}</h2>
              {report.wordpress.findings.length === 0 ? (
                <p className="muted">No exposed paths found.</p>
              ) : (
                <ul>
                  {report.wordpress.findings.map((f) => (
                    <li key={f}>{f}</li>
                  ))}
                </ul>
              )}
            </section>
          )}
        </>
      )}
    </main>
  )
}
