import { useState } from 'react';
import { FiDatabase, FiTable, FiTruck, FiMapPin, FiGrid, FiChevronRight, FiChevronDown, FiPlus, FiTrash2, FiEdit2 } from 'react-icons/fi';

const tableIcons = {
  table: <FiTable size={13} />,
  truck: <FiTruck size={13} />,
  route: <FiMapPin size={13} />,
  map: <FiMapPin size={13} />,
  grid: <FiGrid size={13} />,
};

export default function DatastorePanel({ tables, selectedTable, onSelectTable }) {
  const [expanded, setExpanded] = useState(true);
  const [expandedTables, setExpandedTables] = useState({});

  const toggleTable = (name) => {
    setExpandedTables(prev => ({ ...prev, [name]: !prev[name] }));
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="odl-panel-header">
        <FiDatabase size={13} />
        <span>Datastore</span>
        <div style={{ flex: 1 }} />
        <button className="odl-toolbar-btn" title="Add Table" style={{ padding: '1px 3px' }}>
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
          <FiDatabase size={13} style={{ marginRight: 4, color: '#4a7ab5' }} />
          Spreadsheet (5 tables)
        </div>
        {expanded && tables.map((table, i) => (
          <div key={i}>
            <div
              className={`odl-tree-node ${selectedTable === table.name ? 'selected' : ''}`}
              style={{ paddingLeft: 20 }}
              onClick={() => {
                onSelectTable?.(table.name);
                toggleTable(table.name);
              }}
            >
              <span className="odl-tree-toggle">
                {expandedTables[table.name] ? <FiChevronDown /> : <FiChevronRight />}
              </span>
              <span style={{ marginRight: 4, color: selectedTable === table.name ? 'white' : '#4a7ab5' }}>
                {tableIcons[table.icon] || <FiTable size={13} />}
              </span>
              <span style={{ flex: 1 }}>{table.name}</span>
              <span style={{
                fontSize: 10,
                color: selectedTable === table.name ? 'rgba(255,255,255,0.7)' : '#999',
                marginRight: 4
              }}>
                {table.rows}r x {table.columns}c
              </span>
            </div>
            {expandedTables[table.name] && (
              <div style={{ paddingLeft: 48 }}>
                <div className="odl-tree-node" style={{ fontSize: 11, color: '#666' }}>
                  <FiEdit2 size={10} style={{ marginRight: 4 }} /> Edit schema
                </div>
                <div className="odl-tree-node" style={{ fontSize: 11, color: '#666' }}>
                  <FiTrash2 size={10} style={{ marginRight: 4 }} /> Delete table
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
