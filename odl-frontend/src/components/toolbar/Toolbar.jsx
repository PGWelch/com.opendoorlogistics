import {
  FiFile, FiSave, FiFolderPlus, FiUpload, FiDownload,
  FiPlay, FiSquare, FiMap, FiGrid, FiCalendar,
  FiZoomIn, FiZoomOut, FiMaximize2,
  FiNavigation, FiCrosshair, FiLayers,
  FiSettings, FiRefreshCw, FiTruck
} from 'react-icons/fi';

export default function Toolbar({ activeView, onViewChange, onAction }) {
  return (
    <div className="odl-toolbar">
      {/* File operations */}
      <button className="odl-toolbar-btn" title="New Spreadsheet" onClick={() => onAction?.('new')}>
        <FiFile size={14} /> New
      </button>
      <button className="odl-toolbar-btn" title="Open Spreadsheet" onClick={() => onAction?.('open')}>
        <FiFolderPlus size={14} /> Open
      </button>
      <button className="odl-toolbar-btn" title="Save" onClick={() => onAction?.('save')}>
        <FiSave size={14} /> Save
      </button>
      <button className="odl-toolbar-btn" title="Import Data" onClick={() => onAction?.('import')}>
        <FiUpload size={14} />
      </button>
      <button className="odl-toolbar-btn" title="Export Data" onClick={() => onAction?.('export')}>
        <FiDownload size={14} />
      </button>

      <div className="odl-toolbar-separator" />

      {/* View switches */}
      <button
        className={`odl-toolbar-btn ${activeView === 'map' ? 'active' : ''}`}
        title="Map View"
        onClick={() => onViewChange?.('map')}
      >
        <FiMap size={14} /> Map
      </button>
      <button
        className={`odl-toolbar-btn ${activeView === 'table' ? 'active' : ''}`}
        title="Table View"
        onClick={() => onViewChange?.('table')}
      >
        <FiGrid size={14} /> Table
      </button>
      <button
        className={`odl-toolbar-btn ${activeView === 'schedule' ? 'active' : ''}`}
        title="Schedule View"
        onClick={() => onViewChange?.('schedule')}
      >
        <FiCalendar size={14} /> Schedule
      </button>
      <button
        className={`odl-toolbar-btn ${activeView === 'reports' ? 'active' : ''}`}
        title="Reports"
        onClick={() => onViewChange?.('reports')}
      >
        <FiLayers size={14} /> Reports
      </button>
      <button
        className={`odl-toolbar-btn ${activeView === 'optimizer' ? 'active' : ''}`}
        title="Route Optimizer"
        onClick={() => onViewChange?.('optimizer')}
      >
        <FiTruck size={14} /> Optimizer
      </button>
      <button
        className={`odl-toolbar-btn ${activeView === 'geocoder' ? 'active' : ''}`}
        title="Geocoder"
        onClick={() => onViewChange?.('geocoder')}
      >
        <FiCrosshair size={14} /> Geocoder
      </button>

      <div className="odl-toolbar-separator" />

      {/* Map controls */}
      <button className="odl-toolbar-btn" title="Zoom In" onClick={() => onAction?.('zoomIn')}>
        <FiZoomIn size={14} />
      </button>
      <button className="odl-toolbar-btn" title="Zoom Out" onClick={() => onAction?.('zoomOut')}>
        <FiZoomOut size={14} />
      </button>
      <button className="odl-toolbar-btn" title="Fit All" onClick={() => onAction?.('fitAll')}>
        <FiMaximize2 size={14} />
      </button>

      <div className="odl-toolbar-separator" />

      {/* Run controls */}
      <button className="odl-toolbar-btn" title="Run Script" onClick={() => onAction?.('run')}
        style={{ color: 'var(--odl-success)' }}>
        <FiPlay size={14} /> Run
      </button>
      <button className="odl-toolbar-btn" title="Stop" onClick={() => onAction?.('stop')}
        style={{ color: 'var(--odl-error)' }}>
        <FiSquare size={14} />
      </button>
      <button className="odl-toolbar-btn" title="Refresh" onClick={() => onAction?.('refresh')}>
        <FiRefreshCw size={14} />
      </button>

      <div style={{ flex: 1 }} />

      <button className="odl-toolbar-btn" title="Settings" onClick={() => onAction?.('settings')}>
        <FiSettings size={14} />
      </button>
    </div>
  );
}
