import { FiX, FiExternalLink } from 'react-icons/fi';

export default function AboutDialog({ onClose }) {
  return (
    <div className="odl-dialog-overlay" onClick={onClose}>
      <div className="odl-dialog" style={{ width: 450 }} onClick={e => e.stopPropagation()}>
        <div className="odl-dialog-title">
          <span>About ODL Studio</span>
          <button className="internal-frame-title-btn" onClick={onClose}><FiX size={10} /></button>
        </div>
        <div className="odl-dialog-body" style={{ textAlign: 'center' }}>
          <div style={{
            width: 64, height: 64, borderRadius: 12,
            background: 'linear-gradient(135deg, #4a7ab5, #2ecc71)',
            margin: '0 auto 12px',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: 'white', fontSize: 28, fontWeight: 700,
          }}>
            ODL
          </div>
          <h2 style={{ fontSize: 18, margin: '0 0 4px' }}>Open Door Logistics Studio</h2>
          <p style={{ fontSize: 12, color: '#666', margin: '0 0 12px' }}>Web Frontend Edition</p>
          <p style={{ fontSize: 12, lineHeight: 1.6 }}>
            Free, open source software for customer mapping, sales territory design
            &amp; management, vehicle fleet scheduling &amp; logistics.
          </p>
          <div style={{
            margin: '16px 0', padding: '8px 12px',
            background: '#f5f5f5', borderRadius: 4, fontSize: 11,
            textAlign: 'left', lineHeight: 1.8,
          }}>
            <div><strong>License:</strong> GNU LGPL v3</div>
            <div><strong>Original:</strong> Java Swing Desktop Application</div>
            <div><strong>Core Engine:</strong> ODL Core with jsprit VRP, GraphHopper routing</div>
            <div><strong>Maps:</strong> Leaflet + OpenStreetMap</div>
          </div>
          <p style={{ fontSize: 11, color: '#999' }}>
            www.opendoorlogistics.com
          </p>
        </div>
        <div className="odl-dialog-footer">
          <button className="odl-btn" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}
