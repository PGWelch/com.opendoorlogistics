import { useState, useMemo } from 'react';
import { FiTable, FiSearch, FiFilter, FiDownload, FiPlus, FiTrash2, FiChevronUp, FiChevronDown } from 'react-icons/fi';

export default function DataGrid({ tableName, data, onDataChange }) {
  const [sortCol, setSortCol] = useState(null);
  const [sortDir, setSortDir] = useState('asc');
  const [filter, setFilter] = useState('');
  const [selectedRows, setSelectedRows] = useState(new Set());
  const [editCell, setEditCell] = useState(null);

  const columns = useMemo(() => {
    if (!data || data.length === 0) return [];
    return Object.keys(data[0]);
  }, [data]);

  const filteredData = useMemo(() => {
    if (!data) return [];
    let result = [...data];
    if (filter) {
      const f = filter.toLowerCase();
      result = result.filter(row =>
        Object.values(row).some(v => String(v).toLowerCase().includes(f))
      );
    }
    if (sortCol) {
      result.sort((a, b) => {
        const va = a[sortCol], vb = b[sortCol];
        if (typeof va === 'number' && typeof vb === 'number') {
          return sortDir === 'asc' ? va - vb : vb - va;
        }
        const sa = String(va), sb = String(vb);
        return sortDir === 'asc' ? sa.localeCompare(sb) : sb.localeCompare(sa);
      });
    }
    return result;
  }, [data, filter, sortCol, sortDir]);

  const handleSort = (col) => {
    if (sortCol === col) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc');
    } else {
      setSortCol(col);
      setSortDir('asc');
    }
  };

  const toggleRow = (i) => {
    setSelectedRows(prev => {
      const next = new Set(prev);
      if (next.has(i)) next.delete(i); else next.add(i);
      return next;
    });
  };

  const formatValue = (val) => {
    if (val === null || val === undefined) return '';
    if (typeof val === 'number') return val.toLocaleString();
    return String(val);
  };

  return (
    <div className="internal-frame" style={{ height: '100%' }}>
      <div className="internal-frame-title">
        <span><FiTable size={12} style={{ marginRight: 4 }} /> {tableName || 'Table View'}</span>
        <div className="internal-frame-title-buttons">
          <button className="internal-frame-title-btn" title="Minimize">_</button>
          <button className="internal-frame-title-btn" title="Maximize">[]</button>
        </div>
      </div>

      {/* Table toolbar */}
      <div className="odl-toolbar" style={{ minHeight: 28, padding: '2px 6px' }}>
        <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
          <FiSearch size={12} style={{ position: 'absolute', left: 6, color: '#999' }} />
          <input
            className="odl-input"
            type="text"
            placeholder="Filter..."
            value={filter}
            onChange={e => setFilter(e.target.value)}
            style={{ paddingLeft: 22, width: 180 }}
          />
        </div>
        <div className="odl-toolbar-separator" />
        <button className="odl-toolbar-btn" title="Add Row"><FiPlus size={13} /> Add</button>
        <button className="odl-toolbar-btn" title="Delete Selected"
          disabled={selectedRows.size === 0}>
          <FiTrash2 size={13} /> Delete
        </button>
        <div className="odl-toolbar-separator" />
        <button className="odl-toolbar-btn" title="Export"><FiDownload size={13} /> Export</button>
        <div style={{ flex: 1 }} />
        <span style={{ fontSize: 11, color: '#666' }}>
          {filteredData.length} rows {filter && `(of ${data?.length || 0})`} | {columns.length} columns
        </span>
      </div>

      {/* Grid */}
      <div className="internal-frame-content" style={{ overflow: 'auto' }}>
        <table style={{
          width: '100%', borderCollapse: 'collapse', fontSize: 12, tableLayout: 'auto',
        }}>
          <thead>
            <tr>
              <th style={{
                position: 'sticky', top: 0, background: 'var(--odl-grid-header)',
                borderBottom: '2px solid var(--odl-border)', padding: '4px 6px',
                width: 30, textAlign: 'center', zIndex: 1
              }}>
                <input
                  type="checkbox"
                  onChange={(e) => {
                    if (e.target.checked) {
                      setSelectedRows(new Set(filteredData.map((_, i) => i)));
                    } else {
                      setSelectedRows(new Set());
                    }
                  }}
                  checked={selectedRows.size > 0 && selectedRows.size === filteredData.length}
                />
              </th>
              <th style={{
                position: 'sticky', top: 0, background: 'var(--odl-grid-header)',
                borderBottom: '2px solid var(--odl-border)', padding: '4px 6px',
                width: 40, textAlign: 'center', color: '#999', zIndex: 1
              }}>#</th>
              {columns.map(col => (
                <th
                  key={col}
                  onClick={() => handleSort(col)}
                  style={{
                    position: 'sticky', top: 0, background: 'var(--odl-grid-header)',
                    borderBottom: '2px solid var(--odl-border)', padding: '4px 8px',
                    textAlign: 'left', cursor: 'pointer', whiteSpace: 'nowrap',
                    fontWeight: 600, userSelect: 'none', zIndex: 1,
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    {col}
                    {sortCol === col && (
                      sortDir === 'asc' ? <FiChevronUp size={11} /> : <FiChevronDown size={11} />
                    )}
                  </div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filteredData.map((row, i) => (
              <tr
                key={i}
                style={{
                  background: selectedRows.has(i)
                    ? 'var(--odl-highlight-light)'
                    : i % 2 === 0 ? 'white' : 'var(--odl-grid-row-alt)',
                  cursor: 'pointer',
                }}
                onClick={() => toggleRow(i)}
                onMouseEnter={e => {
                  if (!selectedRows.has(i)) e.currentTarget.style.background = 'var(--odl-grid-row-hover)';
                }}
                onMouseLeave={e => {
                  if (!selectedRows.has(i)) {
                    e.currentTarget.style.background = i % 2 === 0 ? 'white' : 'var(--odl-grid-row-alt)';
                  }
                }}
              >
                <td style={{ padding: '3px 6px', borderBottom: '1px solid #eee', textAlign: 'center' }}>
                  <input type="checkbox" checked={selectedRows.has(i)} onChange={() => toggleRow(i)} />
                </td>
                <td style={{ padding: '3px 6px', borderBottom: '1px solid #eee', textAlign: 'center', color: '#999', fontSize: 11 }}>
                  {i + 1}
                </td>
                {columns.map(col => (
                  <td
                    key={col}
                    style={{
                      padding: '3px 8px', borderBottom: '1px solid #eee',
                      whiteSpace: 'nowrap', maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis',
                      textAlign: typeof row[col] === 'number' ? 'right' : 'left',
                    }}
                    onDoubleClick={() => setEditCell({ row: i, col })}
                  >
                    {editCell?.row === i && editCell?.col === col ? (
                      <input
                        className="odl-input"
                        defaultValue={formatValue(row[col])}
                        autoFocus
                        onBlur={() => setEditCell(null)}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' || e.key === 'Escape') setEditCell(null);
                        }}
                        style={{ width: '100%', padding: '1px 4px', margin: '-2px 0' }}
                      />
                    ) : (
                      formatValue(row[col])
                    )}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
