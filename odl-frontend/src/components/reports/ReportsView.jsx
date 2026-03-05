import { useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip as ReTooltip, ResponsiveContainer, PieChart, Pie, Cell, LineChart, Line, Legend } from 'recharts';
import { FiBarChart2, FiPieChart, FiTrendingUp } from 'react-icons/fi';

const COLORS = ['#e74c3c', '#3498db', '#2ecc71', '#9b59b6', '#f39c12', '#1abc9c', '#e67e22', '#34495e', '#7f8c8d'];

export default function ReportsView({ customers, routes, territories }) {
  const [activeChart, setActiveChart] = useState('territory');

  // Territory distribution data
  const territoryData = territories?.map(t => ({
    name: t.name,
    customers: t.customerCount,
    quantity: t.totalQuantity,
    color: t.color,
  })) || [];

  // Route performance data
  const routeData = routes?.map(r => ({
    name: r.vehicleName,
    distance: r.totalDistance,
    stops: r.stops.length,
    load: r.totalLoad,
    color: r.color,
  })) || [];

  // Quantity by customer (top 10)
  const customerData = (customers || [])
    .sort((a, b) => b.quantity - a.quantity)
    .slice(0, 10)
    .map(c => ({ name: c.name, quantity: c.quantity }));

  return (
    <div className="internal-frame" style={{ height: '100%' }}>
      <div className="internal-frame-title">
        <span><FiBarChart2 size={12} style={{ marginRight: 4 }} /> Reports & Charts</span>
        <div className="internal-frame-title-buttons">
          <button className="internal-frame-title-btn" title="Minimize">_</button>
          <button className="internal-frame-title-btn" title="Maximize">[]</button>
        </div>
      </div>

      {/* Chart tabs */}
      <div className="odl-tabs">
        <div className={`odl-tab ${activeChart === 'territory' ? 'active' : ''}`}
          onClick={() => setActiveChart('territory')}>
          <FiPieChart size={11} style={{ marginRight: 4 }} /> Territory Analysis
        </div>
        <div className={`odl-tab ${activeChart === 'routes' ? 'active' : ''}`}
          onClick={() => setActiveChart('routes')}>
          <FiBarChart2 size={11} style={{ marginRight: 4 }} /> Route Performance
        </div>
        <div className={`odl-tab ${activeChart === 'customers' ? 'active' : ''}`}
          onClick={() => setActiveChart('customers')}>
          <FiTrendingUp size={11} style={{ marginRight: 4 }} /> Customer Analysis
        </div>
      </div>

      <div className="internal-frame-content" style={{ padding: 16, overflow: 'auto' }}>
        {/* Territory Analysis */}
        {activeChart === 'territory' && (
          <div>
            <h3 style={{ fontSize: 14, marginBottom: 12 }}>Territory Distribution</h3>
            <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap' }}>
              <div style={{ flex: '1 1 350px', height: 300 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={territoryData}
                      dataKey="quantity"
                      nameKey="name"
                      cx="50%" cy="50%"
                      outerRadius={100}
                      label={({ name, percent }) => `${name} (${(percent * 100).toFixed(0)}%)`}
                    >
                      {territoryData.map((entry, i) => (
                        <Cell key={i} fill={entry.color || COLORS[i % COLORS.length]} />
                      ))}
                    </Pie>
                    <ReTooltip />
                  </PieChart>
                </ResponsiveContainer>
              </div>
              <div style={{ flex: '1 1 350px', height: 300 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={territoryData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                    <YAxis tick={{ fontSize: 11 }} />
                    <ReTooltip />
                    <Legend />
                    <Bar dataKey="customers" fill="#4a7ab5" name="Customers" />
                    <Bar dataKey="quantity" fill="#f39c12" name="Total Quantity" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>
        )}

        {/* Route Performance */}
        {activeChart === 'routes' && (
          <div>
            <h3 style={{ fontSize: 14, marginBottom: 12 }}>Route Performance Comparison</h3>
            <div style={{ height: 300, marginBottom: 24 }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={routeData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                  <YAxis tick={{ fontSize: 11 }} />
                  <ReTooltip />
                  <Legend />
                  <Bar dataKey="distance" fill="#e74c3c" name="Distance (km)" />
                  <Bar dataKey="load" fill="#3498db" name="Load (units)" />
                </BarChart>
              </ResponsiveContainer>
            </div>

            {/* Route summary table */}
            <div className="odl-panel">
              <div className="odl-panel-header">Route Details</div>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
                <thead>
                  <tr style={{ background: 'var(--odl-grid-header)' }}>
                    <th style={{ padding: '4px 8px', textAlign: 'left' }}>Vehicle</th>
                    <th style={{ padding: '4px 8px', textAlign: 'right' }}>Stops</th>
                    <th style={{ padding: '4px 8px', textAlign: 'right' }}>Distance (km)</th>
                    <th style={{ padding: '4px 8px', textAlign: 'right' }}>Duration</th>
                    <th style={{ padding: '4px 8px', textAlign: 'right' }}>Load</th>
                    <th style={{ padding: '4px 8px', textAlign: 'right' }}>Utilization</th>
                  </tr>
                </thead>
                <tbody>
                  {routes?.map((r, i) => {
                    const vehicle = { capacity: r.vehicleId === 'V003' || r.vehicleId === 'V004' ? 1200 : r.vehicleId === 'V005' ? 250 : 500 };
                    const util = Math.round((r.totalLoad / vehicle.capacity) * 100);
                    return (
                      <tr key={i} style={{ borderBottom: '1px solid #eee' }}>
                        <td style={{ padding: '3px 8px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                            <div style={{ width: 10, height: 10, borderRadius: 2, background: r.color }} />
                            {r.vehicleName}
                          </div>
                        </td>
                        <td style={{ padding: '3px 8px', textAlign: 'right' }}>{r.stops.length}</td>
                        <td style={{ padding: '3px 8px', textAlign: 'right' }}>{r.totalDistance}</td>
                        <td style={{ padding: '3px 8px', textAlign: 'right' }}>{r.totalTime}</td>
                        <td style={{ padding: '3px 8px', textAlign: 'right' }}>{r.totalLoad}</td>
                        <td style={{ padding: '3px 8px', textAlign: 'right' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6, justifyContent: 'flex-end' }}>
                            <div className="odl-progress" style={{ width: 60, height: 12 }}>
                              <div className="odl-progress-bar" style={{
                                width: `${Math.min(util, 100)}%`,
                                background: util > 100 ? 'var(--odl-error)' : util > 80 ? 'var(--odl-warning)' : undefined,
                              }} />
                            </div>
                            <span style={{ fontSize: 11, color: util > 100 ? 'var(--odl-error)' : undefined }}>
                              {util}%
                            </span>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Customer Analysis */}
        {activeChart === 'customers' && (
          <div>
            <h3 style={{ fontSize: 14, marginBottom: 12 }}>Top Customers by Quantity</h3>
            <div style={{ height: 350 }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={customerData} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis type="number" tick={{ fontSize: 11 }} />
                  <YAxis dataKey="name" type="category" tick={{ fontSize: 11 }} width={120} />
                  <ReTooltip />
                  <Bar dataKey="quantity" fill="#4a7ab5" name="Quantity">
                    {customerData.map((_, i) => (
                      <Cell key={i} fill={COLORS[i % COLORS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
