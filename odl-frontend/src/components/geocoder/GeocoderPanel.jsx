import { useState } from 'react';
import { FiMapPin, FiPlay, FiCheck, FiX, FiAlertTriangle } from 'react-icons/fi';

export default function GeocoderPanel({ customers }) {
  const [results, setResults] = useState(null);
  const [running, setRunning] = useState(false);
  const [provider, setProvider] = useState('postcode');

  const runGeocode = () => {
    setRunning(true);
    setTimeout(() => {
      const res = customers?.map(c => ({
        ...c,
        geocodeStatus: Math.random() > 0.1 ? 'success' : 'failed',
        confidence: Math.random() > 0.1 ? (0.85 + Math.random() * 0.15).toFixed(2) : (0.3 + Math.random() * 0.3).toFixed(2),
      })) || [];
      setResults(res);
      setRunning(false);
    }, 1500);
  };

  const successCount = results?.filter(r => r.geocodeStatus === 'success').length || 0;
  const failCount = results?.filter(r => r.geocodeStatus === 'failed').length || 0;

  return (
    <div className="internal-frame" style={{ height: '100%' }}>
      <div className="internal-frame-title">
        <span><FiMapPin size={12} style={{ marginRight: 4 }} /> Geocoder</span>
        <div className="internal-frame-title-buttons">
          <button className="internal-frame-title-btn" title="Minimize">_</button>
          <button className="internal-frame-title-btn" title="Maximize">[]</button>
        </div>
      </div>
      <div className="internal-frame-content" style={{ padding: 16, overflow: 'auto' }}>
        <div style={{ maxWidth: 700 }}>
          {/* Config */}
          <div className="odl-panel" style={{ marginBottom: 12 }}>
            <div className="odl-panel-header"><FiMapPin size={12} /> Geocoding Configuration</div>
            <div style={{ padding: 12, display: 'flex', gap: 16, alignItems: 'flex-end' }}>
              <label style={{ fontSize: 12 }}>
                Provider
                <select className="odl-select" value={provider} onChange={e => setProvider(e.target.value)}
                  style={{ display: 'block', marginTop: 2 }}>
                  <option value="postcode">UK Postcode Database</option>
                  <option value="nominatim">Nominatim (OpenStreetMap)</option>
                  <option value="google">Google Geocoding API</option>
                </select>
              </label>
              <label style={{ fontSize: 12 }}>
                Address column
                <select className="odl-select" style={{ display: 'block', marginTop: 2 }}>
                  <option>address</option>
                  <option>postcode</option>
                </select>
              </label>
              <button className="odl-btn odl-btn-primary" onClick={runGeocode} disabled={running}
                style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                <FiPlay size={12} /> {running ? 'Geocoding...' : 'Run Geocoder'}
              </button>
            </div>
          </div>

          {/* Results */}
          {results && (
            <>
              <div style={{ display: 'flex', gap: 12, marginBottom: 12 }}>
                <div className="odl-panel" style={{ flex: 1, padding: 12, textAlign: 'center' }}>
                  <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--odl-success)' }}>{successCount}</div>
                  <div style={{ fontSize: 11, color: '#666' }}>Geocoded</div>
                </div>
                <div className="odl-panel" style={{ flex: 1, padding: 12, textAlign: 'center' }}>
                  <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--odl-error)' }}>{failCount}</div>
                  <div style={{ fontSize: 11, color: '#666' }}>Failed</div>
                </div>
                <div className="odl-panel" style={{ flex: 1, padding: 12, textAlign: 'center' }}>
                  <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--odl-highlight)' }}>
                    {results.length > 0 ? Math.round((successCount / results.length) * 100) : 0}%
                  </div>
                  <div style={{ fontSize: 11, color: '#666' }}>Success Rate</div>
                </div>
              </div>

              <div className="odl-panel">
                <div className="odl-panel-header">Results</div>
                <div style={{ maxHeight: 300, overflow: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
                    <thead>
                      <tr style={{ background: 'var(--odl-grid-header)' }}>
                        <th style={{ padding: '4px 8px', textAlign: 'left' }}>Status</th>
                        <th style={{ padding: '4px 8px', textAlign: 'left' }}>Name</th>
                        <th style={{ padding: '4px 8px', textAlign: 'left' }}>Address</th>
                        <th style={{ padding: '4px 8px', textAlign: 'right' }}>Lat</th>
                        <th style={{ padding: '4px 8px', textAlign: 'right' }}>Lng</th>
                        <th style={{ padding: '4px 8px', textAlign: 'right' }}>Confidence</th>
                      </tr>
                    </thead>
                    <tbody>
                      {results.map((r, i) => (
                        <tr key={i} style={{ borderBottom: '1px solid #eee' }}>
                          <td style={{ padding: '3px 8px' }}>
                            {r.geocodeStatus === 'success'
                              ? <FiCheck size={12} color="var(--odl-success)" />
                              : <FiX size={12} color="var(--odl-error)" />}
                          </td>
                          <td style={{ padding: '3px 8px' }}>{r.name}</td>
                          <td style={{ padding: '3px 8px', color: '#666' }}>{r.address}</td>
                          <td style={{ padding: '3px 8px', textAlign: 'right' }}>{r.lat.toFixed(4)}</td>
                          <td style={{ padding: '3px 8px', textAlign: 'right' }}>{r.lng.toFixed(4)}</td>
                          <td style={{ padding: '3px 8px', textAlign: 'right' }}>
                            <span style={{
                              color: Number(r.confidence) > 0.8 ? 'var(--odl-success)' : 'var(--odl-warning)',
                              fontWeight: Number(r.confidence) < 0.5 ? 700 : 400,
                            }}>
                              {r.confidence}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
