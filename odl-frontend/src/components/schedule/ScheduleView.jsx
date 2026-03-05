import { useMemo } from 'react';
import { FiCalendar, FiClock } from 'react-icons/fi';

function timeToMinutes(t) {
  const [h, m] = t.split(':').map(Number);
  return h * 60 + m;
}

function minutesToTime(m) {
  const h = Math.floor(m / 60);
  const min = m % 60;
  return `${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`;
}

export default function ScheduleView({ routes, customers, vehicles }) {
  const startHour = 6;
  const endHour = 19;
  const totalMinutes = (endHour - startHour) * 60;
  const hourWidth = 80;

  const hours = useMemo(() => {
    const arr = [];
    for (let h = startHour; h <= endHour; h++) arr.push(h);
    return arr;
  }, []);

  return (
    <div className="internal-frame" style={{ height: '100%' }}>
      <div className="internal-frame-title">
        <span><FiCalendar size={12} style={{ marginRight: 4 }} /> Schedule View - Gantt Chart</span>
        <div className="internal-frame-title-buttons">
          <button className="internal-frame-title-btn" title="Minimize">_</button>
          <button className="internal-frame-title-btn" title="Maximize">[]</button>
        </div>
      </div>
      <div className="internal-frame-content" style={{ overflow: 'auto', padding: 0 }}>
        {/* Hour header */}
        <div style={{
          display: 'flex', position: 'sticky', top: 0, zIndex: 2,
          background: 'var(--odl-grid-header)', borderBottom: '2px solid var(--odl-border)',
        }}>
          <div style={{
            minWidth: 140, padding: '6px 8px', fontWeight: 600, fontSize: 12,
            borderRight: '1px solid var(--odl-border)', background: 'var(--odl-grid-header)',
            position: 'sticky', left: 0, zIndex: 3,
          }}>
            Vehicle
          </div>
          <div style={{ display: 'flex', flex: 1 }}>
            {hours.map(h => (
              <div key={h} style={{
                minWidth: hourWidth, borderRight: '1px solid #ddd',
                padding: '6px 4px', fontSize: 11, textAlign: 'center', fontWeight: 500,
              }}>
                {`${String(h).padStart(2, '0')}:00`}
              </div>
            ))}
          </div>
        </div>

        {/* Vehicle rows */}
        {routes?.map((route, ri) => {
          const vehicle = vehicles?.find(v => v.id === route.vehicleId);
          return (
            <div key={ri} style={{
              display: 'flex', borderBottom: '1px solid #eee',
              background: ri % 2 === 0 ? 'white' : '#f9f9fc',
              minHeight: 48,
            }}>
              {/* Vehicle label */}
              <div style={{
                minWidth: 140, padding: '6px 8px', fontSize: 12,
                borderRight: '1px solid var(--odl-border)', display: 'flex',
                flexDirection: 'column', justifyContent: 'center',
                position: 'sticky', left: 0, zIndex: 1,
                background: ri % 2 === 0 ? 'white' : '#f9f9fc',
              }}>
                <div style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: 4 }}>
                  <div style={{ width: 10, height: 10, borderRadius: 2, background: route.color }} />
                  {route.vehicleName}
                </div>
                <div style={{ fontSize: 10, color: '#888' }}>
                  {route.totalDistance}km | {route.totalLoad} units
                </div>
              </div>

              {/* Timeline */}
              <div style={{ flex: 1, position: 'relative', minHeight: 48 }}>
                {/* Grid lines */}
                {hours.map(h => (
                  <div key={h} style={{
                    position: 'absolute',
                    left: (h - startHour) * hourWidth,
                    top: 0, bottom: 0,
                    borderLeft: '1px solid #eee',
                    width: 0,
                  }} />
                ))}

                {/* Vehicle active time bar */}
                {vehicle && (
                  <div style={{
                    position: 'absolute',
                    left: ((timeToMinutes(vehicle.startTime) - startHour * 60) / 60) * hourWidth,
                    width: ((timeToMinutes(vehicle.endTime) - timeToMinutes(vehicle.startTime)) / 60) * hourWidth,
                    top: 4, height: 6,
                    background: '#e0e0e0', borderRadius: 3,
                  }} />
                )}

                {/* Stop blocks */}
                {route.stops.map((stop, si) => {
                  const cust = customers?.find(c => c.id === stop.customerId);
                  const arrMin = timeToMinutes(stop.arrival);
                  const depMin = timeToMinutes(stop.departure);
                  const left = ((arrMin - startHour * 60) / 60) * hourWidth;
                  const width = Math.max(((depMin - arrMin) / 60) * hourWidth, 50);

                  return (
                    <div
                      key={si}
                      title={`${cust?.name || 'Stop'} (${stop.arrival}-${stop.departure})`}
                      style={{
                        position: 'absolute',
                        left, width,
                        top: 14, height: 26,
                        background: route.color,
                        borderRadius: 3,
                        color: 'white', fontSize: 10,
                        padding: '2px 4px',
                        overflow: 'hidden', whiteSpace: 'nowrap',
                        textOverflow: 'ellipsis',
                        display: 'flex', flexDirection: 'column',
                        justifyContent: 'center',
                        cursor: 'pointer',
                        boxShadow: '0 1px 2px rgba(0,0,0,0.2)',
                        opacity: 0.9,
                      }}
                    >
                      <div style={{ fontWeight: 600, lineHeight: 1.2 }}>{cust?.name || `Stop ${si + 1}`}</div>
                      <div style={{ lineHeight: 1.2, opacity: 0.8 }}>{stop.arrival}</div>
                    </div>
                  );
                })}

                {/* Travel lines between stops */}
                {route.stops.length > 1 && route.stops.slice(0, -1).map((stop, si) => {
                  const nextStop = route.stops[si + 1];
                  const depMin = timeToMinutes(stop.departure);
                  const nextArrMin = timeToMinutes(nextStop.arrival);
                  const left = ((depMin - startHour * 60) / 60) * hourWidth;
                  const width = ((nextArrMin - depMin) / 60) * hourWidth;
                  return (
                    <div key={`t-${si}`} style={{
                      position: 'absolute', left, width,
                      top: 25, height: 2,
                      background: route.color, opacity: 0.4,
                    }} />
                  );
                })}
              </div>
            </div>
          );
        })}

        {/* Summary row */}
        {routes && (
          <div style={{
            display: 'flex', borderTop: '2px solid var(--odl-border)',
            background: '#f5f5f5', padding: '8px',
          }}>
            <div style={{ minWidth: 140, fontWeight: 600, fontSize: 12 }}>
              Summary
            </div>
            <div style={{ display: 'flex', gap: 24, fontSize: 12 }}>
              <span>Vehicles: {routes.length}</span>
              <span>Total Distance: {routes.reduce((s, r) => s + r.totalDistance, 0)}km</span>
              <span>Total Stops: {routes.reduce((s, r) => s + r.stops.length, 0)}</span>
              <span>Total Load: {routes.reduce((s, r) => s + r.totalLoad, 0)} units</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
