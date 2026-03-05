import { useState } from 'react';
import { FiPlay, FiCode, FiChevronRight, FiChevronDown, FiPlus, FiUpload, FiDownload, FiSettings, FiMapPin, FiBarChart2, FiCpu } from 'react-icons/fi';

const scriptTypeIcons = {
  import: <FiUpload size={12} />,
  export: <FiDownload size={12} />,
  optimization: <FiCpu size={12} />,
  analysis: <FiSettings size={12} />,
  calculation: <FiBarChart2 size={12} />,
  report: <FiBarChart2 size={12} />,
  geocoding: <FiMapPin size={12} />,
  visualization: <FiBarChart2 size={12} />,
};

const scriptTypeColors = {
  import: '#2196f3',
  export: '#4caf50',
  optimization: '#ff9800',
  analysis: '#9c27b0',
  calculation: '#607d8b',
  report: '#e91e63',
  geocoding: '#00bcd4',
  visualization: '#795548',
};

export default function ScriptsPanel({ scripts, onRunScript }) {
  const [expanded, setExpanded] = useState(true);
  const [selectedScript, setSelectedScript] = useState(null);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="odl-panel-header">
        <FiCode size={13} />
        <span>Scripts</span>
        <div style={{ flex: 1 }} />
        <button className="odl-toolbar-btn" title="New Script" style={{ padding: '1px 3px' }}>
          <FiPlus size={12} />
        </button>
      </div>
      <div style={{ flex: 1, overflow: 'auto', padding: '4px 0' }}>
        <div
          className="odl-tree-node"
          style={{ paddingLeft: 4, fontWeight: 600 }}
          onClick={() => setExpanded(!expanded)}
        >
          <span className="odl-tree-toggle">
            {expanded ? <FiChevronDown /> : <FiChevronRight />}
          </span>
          <FiCode size={13} style={{ marginRight: 4, color: '#4a7ab5' }} />
          Scripts ({scripts.length})
        </div>
        {expanded && scripts.map((script) => (
          <div
            key={script.id}
            className={`odl-tree-node ${selectedScript === script.id ? 'selected' : ''}`}
            style={{ paddingLeft: 24 }}
            onClick={() => setSelectedScript(script.id)}
            onDoubleClick={() => onRunScript?.(script)}
          >
            <span style={{
              marginRight: 5,
              color: selectedScript === script.id ? 'white' : (scriptTypeColors[script.type] || '#666')
            }}>
              {scriptTypeIcons[script.type] || <FiCode size={12} />}
            </span>
            <span style={{ flex: 1 }}>{script.name}</span>
            <button
              className="odl-toolbar-btn"
              style={{
                padding: '0 2px',
                color: selectedScript === script.id ? 'white' : 'var(--odl-success)',
                opacity: selectedScript === script.id ? 1 : 0.5
              }}
              title="Run script"
              onClick={(e) => {
                e.stopPropagation();
                onRunScript?.(script);
              }}
            >
              <FiPlay size={11} />
            </button>
          </div>
        ))}
      </div>
      {selectedScript && (
        <div style={{
          borderTop: '1px solid var(--odl-border)',
          padding: '6px 8px',
          fontSize: 11,
          color: '#666',
          background: '#f8f8f8'
        }}>
          {scripts.find(s => s.id === selectedScript)?.description}
        </div>
      )}
    </div>
  );
}
