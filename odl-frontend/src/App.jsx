import { useState, useCallback } from 'react';
import './styles/odl-theme.css';
import MenuBar from './components/toolbar/MenuBar';
import Toolbar from './components/toolbar/Toolbar';
import DatastorePanel from './components/sidebar/DatastorePanel';
import ScriptsPanel from './components/sidebar/ScriptsPanel';
import MapView from './components/map/MapView';
import DataGrid from './components/grid/DataGrid';
import ScheduleView from './components/schedule/ScheduleView';
import OptimizerPanel from './components/optimizer/OptimizerPanel';
import GeocoderPanel from './components/geocoder/GeocoderPanel';
import ReportsView from './components/reports/ReportsView';
import AboutDialog from './components/dialogs/AboutDialog';
import {
  sampleCustomers, sampleVehicles, sampleRoutes,
  sampleTerritories, sampleScripts, datastoreTables
} from './data/sampleData';

const tableDataMap = {
  Customers: sampleCustomers,
  Vehicles: sampleVehicles,
  Territories: sampleTerritories,
};

export default function App() {
  const [activeView, setActiveView] = useState('map');
  const [selectedTable, setSelectedTable] = useState('Customers');
  const [showAbout, setShowAbout] = useState(false);
  const [sidebarWidth, setSidebarWidth] = useState(240);
  const [sidebarSplit, setSidebarSplit] = useState(0.55);
  const [statusText, setStatusText] = useState('Ready');
  const [isResizing, setIsResizing] = useState(false);
  const [isResizingSplit, setIsResizingSplit] = useState(false);

  const handleMenuAction = useCallback((action) => {
    switch (action) {
      case 'About ODL Studio': setShowAbout(true); break;
      case 'Map View': setActiveView('map'); break;
      case 'Table View': setActiveView('table'); break;
      case 'Schedule View': setActiveView('schedule'); break;
      case 'Vehicle Routing (VRP)': setActiveView('optimizer'); break;
      case 'Geocode Addresses': setActiveView('geocoder'); break;
      default: setStatusText(`Action: ${action}`);
    }
  }, []);

  const handleRunScript = useCallback((script) => {
    setStatusText(`Running script: ${script.name}...`);
    setTimeout(() => setStatusText(`Script "${script.name}" completed.`), 2000);
  }, []);

  const handleSelectTable = useCallback((name) => {
    setSelectedTable(name);
    if (tableDataMap[name]) setActiveView('table');
  }, []);

  const handleSidebarResize = useCallback((e) => {
    e.preventDefault();
    setIsResizing(true);
    const startX = e.clientX;
    const startWidth = sidebarWidth;
    const onMove = (ev) => {
      const newWidth = Math.max(180, Math.min(500, startWidth + ev.clientX - startX));
      setSidebarWidth(newWidth);
    };
    const onUp = () => {
      setIsResizing(false);
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
  }, [sidebarWidth]);

  const handleSplitResize = useCallback((e) => {
    e.preventDefault();
    setIsResizingSplit(true);
    const sidebar = e.currentTarget.parentElement;
    const rect = sidebar.getBoundingClientRect();
    const onMove = (ev) => {
      const relY = ev.clientY - rect.top;
      const ratio = Math.max(0.2, Math.min(0.8, relY / rect.height));
      setSidebarSplit(ratio);
    };
    const onUp = () => {
      setIsResizingSplit(false);
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
  }, []);

  const renderMainContent = () => {
    switch (activeView) {
      case 'map':
        return <MapView customers={sampleCustomers} vehicles={sampleVehicles} routes={sampleRoutes} />;
      case 'table':
        return <DataGrid tableName={selectedTable} data={tableDataMap[selectedTable] || sampleCustomers} />;
      case 'schedule':
        return <ScheduleView routes={sampleRoutes} customers={sampleCustomers} vehicles={sampleVehicles} />;
      case 'optimizer':
        return <OptimizerPanel vehicles={sampleVehicles} customers={sampleCustomers} />;
      case 'geocoder':
        return <GeocoderPanel customers={sampleCustomers} />;
      case 'reports':
        return <ReportsView customers={sampleCustomers} routes={sampleRoutes} territories={sampleTerritories} />;
      default:
        return <MapView customers={sampleCustomers} vehicles={sampleVehicles} routes={sampleRoutes} />;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden' }}>
      <MenuBar onAction={handleMenuAction} />
      <Toolbar activeView={activeView} onViewChange={setActiveView} onAction={handleMenuAction} />

      <div style={{
        display: 'flex', flex: 1, overflow: 'hidden',
        userSelect: (isResizing || isResizingSplit) ? 'none' : undefined,
      }}>
        {/* Sidebar */}
        <div className="odl-sidebar" style={{ width: sidebarWidth, minWidth: sidebarWidth, display: 'flex', flexDirection: 'column' }}>
          <div style={{ height: `${sidebarSplit * 100}%`, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
            <DatastorePanel tables={datastoreTables} selectedTable={selectedTable} onSelectTable={handleSelectTable} />
          </div>
          <div className="odl-splitter odl-splitter-h" onMouseDown={handleSplitResize} />
          <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
            <ScriptsPanel scripts={sampleScripts} onRunScript={handleRunScript} />
          </div>
        </div>

        <div className="odl-splitter" onMouseDown={handleSidebarResize} />

        {/* Main view */}
        <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <div className="odl-tabs">
            {[
              { key: 'map', label: 'Map' },
              { key: 'table', label: `Table: ${selectedTable}` },
              { key: 'schedule', label: 'Schedule' },
              { key: 'reports', label: 'Reports' },
              { key: 'optimizer', label: 'Optimizer' },
              { key: 'geocoder', label: 'Geocoder' },
            ].map(tab => (
              <div key={tab.key} className={`odl-tab ${activeView === tab.key ? 'active' : ''}`}
                onClick={() => setActiveView(tab.key)}>
                {tab.label}
              </div>
            ))}
          </div>
          <div style={{ flex: 1, overflow: 'hidden' }}>
            {renderMainContent()}
          </div>
        </div>
      </div>

      <div className="odl-statusbar">
        <span>{statusText}</span>
        <span>{sampleCustomers.length} customers | {sampleVehicles.length} vehicles | {sampleRoutes.length} routes</span>
      </div>

      {showAbout && <AboutDialog onClose={() => setShowAbout(false)} />}
    </div>
  );
}
