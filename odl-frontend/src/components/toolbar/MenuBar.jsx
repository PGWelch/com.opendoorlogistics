import { useState, useRef, useEffect } from 'react';
import { FiFile, FiEdit, FiPlay, FiMap, FiHelpCircle, FiEye } from 'react-icons/fi';

const menus = [
  {
    label: 'File',
    icon: <FiFile size={12} />,
    items: [
      { label: 'New Spreadsheet', shortcut: 'Ctrl+N' },
      { label: 'Open Spreadsheet...', shortcut: 'Ctrl+O' },
      { separator: true },
      { label: 'Save', shortcut: 'Ctrl+S' },
      { label: 'Save As...' },
      { separator: true },
      { label: 'Import...', shortcut: 'Ctrl+I' },
      { label: 'Export...' },
      { separator: true },
      { label: 'Exit' },
    ]
  },
  {
    label: 'Edit',
    icon: <FiEdit size={12} />,
    items: [
      { label: 'Undo', shortcut: 'Ctrl+Z' },
      { label: 'Redo', shortcut: 'Ctrl+Y' },
      { separator: true },
      { label: 'Cut', shortcut: 'Ctrl+X' },
      { label: 'Copy', shortcut: 'Ctrl+C' },
      { label: 'Paste', shortcut: 'Ctrl+V' },
      { separator: true },
      { label: 'Select All', shortcut: 'Ctrl+A' },
      { label: 'Delete Selected', shortcut: 'Del' },
    ]
  },
  {
    label: 'View',
    icon: <FiEye size={12} />,
    items: [
      { label: 'Map View' },
      { label: 'Table View' },
      { label: 'Schedule View' },
      { separator: true },
      { label: 'Zoom In', shortcut: 'Ctrl+=' },
      { label: 'Zoom Out', shortcut: 'Ctrl+-' },
      { label: 'Fit All', shortcut: 'Ctrl+0' },
    ]
  },
  {
    label: 'Run',
    icon: <FiPlay size={12} />,
    items: [
      { label: 'Run Script...', shortcut: 'F5' },
      { label: 'Run Last Script', shortcut: 'F8' },
      { separator: true },
      { label: 'Vehicle Routing (VRP)' },
      { label: 'Territory Design' },
      { label: 'Geocode Addresses' },
      { separator: true },
      { label: 'Calculate Distances' },
    ]
  },
  {
    label: 'Map',
    icon: <FiMap size={12} />,
    items: [
      { label: 'Show All Points' },
      { label: 'Show Routes' },
      { label: 'Show Territories' },
      { separator: true },
      { label: 'Snapshot Map...' },
      { label: 'Background Map Settings...' },
    ]
  },
  {
    label: 'Help',
    icon: <FiHelpCircle size={12} />,
    items: [
      { label: 'About ODL Studio' },
      { label: 'Documentation' },
      { label: 'Licences' },
    ]
  }
];

export default function MenuBar({ onAction }) {
  const [openMenu, setOpenMenu] = useState(null);
  const menuRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setOpenMenu(null);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="odl-menubar" ref={menuRef}>
      {menus.map((menu, i) => (
        <div
          key={i}
          className="odl-menu-item"
          onMouseDown={() => setOpenMenu(openMenu === i ? null : i)}
          onMouseEnter={() => openMenu !== null && setOpenMenu(i)}
        >
          {menu.label}
          {openMenu === i && (
            <div className="odl-dropdown">
              {menu.items.map((item, j) =>
                item.separator ? (
                  <div key={j} className="odl-dropdown-separator" />
                ) : (
                  <div
                    key={j}
                    className="odl-dropdown-item"
                    onClick={() => {
                      setOpenMenu(null);
                      onAction?.(item.label);
                    }}
                  >
                    <span style={{ flex: 1 }}>{item.label}</span>
                    {item.shortcut && (
                      <span style={{ color: '#999', fontSize: 11 }}>{item.shortcut}</span>
                    )}
                  </div>
                )
              )}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
