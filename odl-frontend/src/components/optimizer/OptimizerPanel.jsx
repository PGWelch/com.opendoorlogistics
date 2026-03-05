import { useState } from 'react';
import { FiTruck, FiPlay, FiSquare, FiSettings, FiCheckCircle, FiAlertCircle } from 'react-icons/fi';

export default function OptimizerPanel({ vehicles, customers }) {
  const [config, setConfig] = useState({
    algorithm: 'jsprit',
    iterations: 2000,
    threads: 4,
    timeLimit: 60,
    vehicleCapacity: true,
    timeWindows: true,
    returnToDepot: true,
    costPerKm: 0.45,
    costPerHour: 15.0,
    fixedCost: 50.0,
  });
  const [status, setStatus] = useState('idle'); // idle, running, complete, error
  const [progress, setProgress] = useState(0);

  const handleRun = () => {
    setStatus('running');
    setProgress(0);
    // Simulate optimization progress
    let p = 0;
    const interval = setInterval(() => {
      p += Math.random() * 15;
      if (p >= 100) {
        p = 100;
        clearInterval(interval);
        setStatus('complete');
      }
      setProgress(Math.min(p, 100));
    }, 300);
  };

  return (
    <div className="internal-frame" style={{ height: '100%' }}>
      <div className="internal-frame-title">
        <span><FiTruck size={12} style={{ marginRight: 4 }} /> Vehicle Route Optimizer (VRP)</span>
        <div className="internal-frame-title-buttons">
          <button className="internal-frame-title-btn" title="Minimize">_</button>
          <button className="internal-frame-title-btn" title="Maximize">[]</button>
        </div>
      </div>
      <div className="internal-frame-content" style={{ padding: 16, overflow: 'auto' }}>
        <div style={{ maxWidth: 600 }}>
          {/* Algorithm selection */}
          <div className="odl-panel" style={{ marginBottom: 12 }}>
            <div className="odl-panel-header">
              <FiSettings size={12} /> Algorithm Configuration
            </div>
            <div style={{ padding: 12, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px 16px' }}>
              <label style={{ fontSize: 12 }}>
                Algorithm
                <select
                  className="odl-select"
                  value={config.algorithm}
                  onChange={e => setConfig(c => ({ ...c, algorithm: e.target.value }))}
                  style={{ display: 'block', width: '100%', marginTop: 2 }}
                >
                  <option value="jsprit">jsprit (Ruin & Recreate)</option>
                  <option value="greedy">Greedy Construction</option>
                  <option value="savings">Clarke-Wright Savings</option>
                </select>
              </label>
              <label style={{ fontSize: 12 }}>
                Max Iterations
                <input
                  className="odl-input"
                  type="number"
                  value={config.iterations}
                  onChange={e => setConfig(c => ({ ...c, iterations: Number(e.target.value) }))}
                  style={{ display: 'block', width: '100%', marginTop: 2 }}
                />
              </label>
              <label style={{ fontSize: 12 }}>
                Threads
                <input
                  className="odl-input"
                  type="number"
                  value={config.threads}
                  onChange={e => setConfig(c => ({ ...c, threads: Number(e.target.value) }))}
                  style={{ display: 'block', width: '100%', marginTop: 2 }}
                />
              </label>
              <label style={{ fontSize: 12 }}>
                Time Limit (sec)
                <input
                  className="odl-input"
                  type="number"
                  value={config.timeLimit}
                  onChange={e => setConfig(c => ({ ...c, timeLimit: Number(e.target.value) }))}
                  style={{ display: 'block', width: '100%', marginTop: 2 }}
                />
              </label>
            </div>
          </div>

          {/* Constraints */}
          <div className="odl-panel" style={{ marginBottom: 12 }}>
            <div className="odl-panel-header">
              <FiAlertCircle size={12} /> Constraints
            </div>
            <div style={{ padding: 12, display: 'flex', flexDirection: 'column', gap: 6 }}>
              <label style={{ fontSize: 12, display: 'flex', alignItems: 'center', gap: 6 }}>
                <input type="checkbox" checked={config.vehicleCapacity}
                  onChange={e => setConfig(c => ({ ...c, vehicleCapacity: e.target.checked }))} />
                Vehicle capacity constraints
              </label>
              <label style={{ fontSize: 12, display: 'flex', alignItems: 'center', gap: 6 }}>
                <input type="checkbox" checked={config.timeWindows}
                  onChange={e => setConfig(c => ({ ...c, timeWindows: e.target.checked }))} />
                Time window constraints
              </label>
              <label style={{ fontSize: 12, display: 'flex', alignItems: 'center', gap: 6 }}>
                <input type="checkbox" checked={config.returnToDepot}
                  onChange={e => setConfig(c => ({ ...c, returnToDepot: e.target.checked }))} />
                Return to depot
              </label>
            </div>
          </div>

          {/* Costs */}
          <div className="odl-panel" style={{ marginBottom: 12 }}>
            <div className="odl-panel-header">
              <FiSettings size={12} /> Cost Parameters
            </div>
            <div style={{ padding: 12, display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '8px 16px' }}>
              <label style={{ fontSize: 12 }}>
                Cost/km (GBP)
                <input className="odl-input" type="number" step="0.01" value={config.costPerKm}
                  onChange={e => setConfig(c => ({ ...c, costPerKm: Number(e.target.value) }))}
                  style={{ display: 'block', width: '100%', marginTop: 2 }} />
              </label>
              <label style={{ fontSize: 12 }}>
                Cost/hour (GBP)
                <input className="odl-input" type="number" step="0.01" value={config.costPerHour}
                  onChange={e => setConfig(c => ({ ...c, costPerHour: Number(e.target.value) }))}
                  style={{ display: 'block', width: '100%', marginTop: 2 }} />
              </label>
              <label style={{ fontSize: 12 }}>
                Fixed cost (GBP)
                <input className="odl-input" type="number" step="0.01" value={config.fixedCost}
                  onChange={e => setConfig(c => ({ ...c, fixedCost: Number(e.target.value) }))}
                  style={{ display: 'block', width: '100%', marginTop: 2 }} />
              </label>
            </div>
          </div>

          {/* Problem summary */}
          <div className="odl-panel" style={{ marginBottom: 12 }}>
            <div className="odl-panel-header">Problem Summary</div>
            <div style={{ padding: 12, fontSize: 12, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 4 }}>
              <span>Customers: <strong>{customers?.length || 0}</strong></span>
              <span>Vehicles: <strong>{vehicles?.length || 0}</strong></span>
              <span>Total demand: <strong>{customers?.reduce((s, c) => s + c.quantity, 0) || 0}</strong></span>
              <span>Total capacity: <strong>{vehicles?.reduce((s, v) => s + v.capacity, 0) || 0}</strong></span>
            </div>
          </div>

          {/* Run controls */}
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <button
              className="odl-btn odl-btn-primary"
              onClick={handleRun}
              disabled={status === 'running'}
              style={{ display: 'flex', alignItems: 'center', gap: 6 }}
            >
              <FiPlay size={13} /> Run Optimization
            </button>
            {status === 'running' && (
              <button className="odl-btn" onClick={() => setStatus('idle')}
                style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--odl-error)' }}>
                <FiSquare size={13} /> Stop
              </button>
            )}
            {status === 'complete' && (
              <span style={{ color: 'var(--odl-success)', display: 'flex', alignItems: 'center', gap: 4, fontSize: 12 }}>
                <FiCheckCircle size={14} /> Optimization complete!
              </span>
            )}
          </div>

          {/* Progress */}
          {status === 'running' && (
            <div style={{ marginTop: 12 }}>
              <div style={{ fontSize: 11, marginBottom: 4, color: '#666' }}>
                Running {config.algorithm}... {Math.round(progress)}%
              </div>
              <div className="odl-progress">
                <div className="odl-progress-bar" style={{ width: `${progress}%` }} />
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
