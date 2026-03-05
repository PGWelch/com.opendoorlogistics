import { useState, useMemo } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Polyline, CircleMarker, Tooltip, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { FiLayers, FiNavigation, FiEye, FiEyeOff } from 'react-icons/fi';

// Fix default marker icon
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

function createColoredIcon(color) {
  return L.divIcon({
    className: '',
    html: `<div style="
      width: 12px; height: 12px;
      background: ${color};
      border: 2px solid white;
      border-radius: 50%;
      box-shadow: 0 1px 3px rgba(0,0,0,0.4);
    "></div>`,
    iconSize: [12, 12],
    iconAnchor: [6, 6],
    popupAnchor: [0, -8],
  });
}

function createDepotIcon() {
  return L.divIcon({
    className: '',
    html: `<div style="
      width: 16px; height: 16px;
      background: #333;
      border: 2px solid #ff0;
      border-radius: 3px;
      box-shadow: 0 1px 4px rgba(0,0,0,0.5);
    "></div>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8],
    popupAnchor: [0, -10],
  });
}

function MapToolbar({ showRoutes, setShowRoutes, showCustomers, setShowCustomers, showTerritories, setShowTerritories }) {
  return (
    <div style={{
      position: 'absolute', top: 8, right: 8, zIndex: 1000,
      background: 'white', border: '1px solid #ccc', borderRadius: 4,
      padding: '4px', display: 'flex', flexDirection: 'column', gap: 2,
      boxShadow: '0 1px 4px rgba(0,0,0,0.2)'
    }}>
      <button
        className="odl-toolbar-btn"
        style={{ fontSize: 11, padding: '2px 6px', color: showCustomers ? 'var(--odl-highlight)' : '#999' }}
        onClick={() => setShowCustomers(!showCustomers)}
        title="Toggle customers"
      >
        {showCustomers ? <FiEye size={12} /> : <FiEyeOff size={12} />} Customers
      </button>
      <button
        className="odl-toolbar-btn"
        style={{ fontSize: 11, padding: '2px 6px', color: showRoutes ? 'var(--odl-highlight)' : '#999' }}
        onClick={() => setShowRoutes(!showRoutes)}
        title="Toggle routes"
      >
        {showRoutes ? <FiEye size={12} /> : <FiEyeOff size={12} />} Routes
      </button>
      <button
        className="odl-toolbar-btn"
        style={{ fontSize: 11, padding: '2px 6px', color: showTerritories ? 'var(--odl-highlight)' : '#999' }}
        onClick={() => setShowTerritories(!showTerritories)}
        title="Toggle territories"
      >
        {showTerritories ? <FiEye size={12} /> : <FiEyeOff size={12} />} Territories
      </button>
    </div>
  );
}

export default function MapView({ customers, vehicles, routes }) {
  const [showRoutes, setShowRoutes] = useState(true);
  const [showCustomers, setShowCustomers] = useState(true);
  const [showTerritories, setShowTerritories] = useState(false);

  const center = [53.5, -1.5]; // UK center

  const routeLines = useMemo(() => {
    if (!routes || !customers) return [];
    return routes.map(route => {
      const vehicle = vehicles?.find(v => v.id === route.vehicleId);
      const points = [];
      if (vehicle) points.push([vehicle.startLat, vehicle.startLng]);
      route.stops.forEach(stop => {
        const cust = customers.find(c => c.id === stop.customerId);
        if (cust) points.push([cust.lat, cust.lng]);
      });
      if (vehicle) points.push([vehicle.startLat, vehicle.startLng]);
      return { points, color: route.color, name: route.vehicleName };
    });
  }, [routes, customers, vehicles]);

  return (
    <div className="internal-frame" style={{ height: '100%' }}>
      <div className="internal-frame-title">
        <span><FiLayers size={12} style={{ marginRight: 4 }} /> Map View - Interactive Map</span>
        <div className="internal-frame-title-buttons">
          <button className="internal-frame-title-btn" title="Minimize">_</button>
          <button className="internal-frame-title-btn" title="Maximize">[]</button>
        </div>
      </div>
      <div className="internal-frame-content" style={{ position: 'relative' }}>
        <MapToolbar
          showRoutes={showRoutes} setShowRoutes={setShowRoutes}
          showCustomers={showCustomers} setShowCustomers={setShowCustomers}
          showTerritories={showTerritories} setShowTerritories={setShowTerritories}
        />
        <MapContainer
          center={center}
          zoom={6}
          style={{ height: '100%', width: '100%' }}
          zoomControl={true}
        >
          <TileLayer
            attribution='&copy; OpenStreetMap contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          {/* Vehicle depots */}
          {vehicles?.map(v => (
            <Marker key={v.id} position={[v.startLat, v.startLng]} icon={createDepotIcon()}>
              <Popup>
                <strong>{v.name}</strong><br />
                Type: {v.type}<br />
                Capacity: {v.capacity}<br />
                Hours: {v.startTime} - {v.endTime}
              </Popup>
            </Marker>
          ))}

          {/* Customer markers */}
          {showCustomers && customers?.map(c => {
            const route = routes?.find(r => r.stops.some(s => s.customerId === c.id));
            const color = route?.color || '#666';
            return (
              <Marker key={c.id} position={[c.lat, c.lng]} icon={createColoredIcon(color)}>
                <Tooltip direction="top" offset={[0, -8]} opacity={0.9}>
                  <strong>{c.name}</strong> — Qty: {c.quantity}
                </Tooltip>
                <Popup>
                  <strong>{c.name}</strong><br />
                  {c.address}<br />
                  {c.postcode}<br />
                  Quantity: {c.quantity}<br />
                  Time: {c.timeWindow}<br />
                  Territory: {c.territory}
                </Popup>
              </Marker>
            );
          })}

          {/* Route polylines */}
          {showRoutes && routeLines.map((route, i) => (
            <Polyline
              key={i}
              positions={route.points}
              color={route.color}
              weight={3}
              opacity={0.7}
              dashArray="8 4"
            >
              <Tooltip sticky>
                {route.name}
              </Tooltip>
            </Polyline>
          ))}

          {/* Territory circles */}
          {showTerritories && customers?.map(c => (
            <CircleMarker
              key={`t-${c.id}`}
              center={[c.lat, c.lng]}
              radius={Math.sqrt(c.quantity) * 2}
              color="transparent"
              fillColor={
                routes?.find(r => r.stops.some(s => s.customerId === c.id))?.color || '#999'
              }
              fillOpacity={0.25}
            />
          ))}
        </MapContainer>

        {/* Legend */}
        {showRoutes && routes && (
          <div style={{
            position: 'absolute', bottom: 8, left: 8, zIndex: 1000,
            background: 'rgba(255,255,255,0.92)', border: '1px solid #ccc',
            borderRadius: 4, padding: '6px 10px', fontSize: 11,
            boxShadow: '0 1px 3px rgba(0,0,0,0.15)'
          }}>
            <div style={{ fontWeight: 600, marginBottom: 3 }}>Routes</div>
            {routes.map((r, i) => (
              <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 1 }}>
                <div style={{ width: 14, height: 3, background: r.color, borderRadius: 1 }} />
                <span>{r.vehicleName} ({r.totalDistance}km)</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
