// Sample data mimicking ODL Studio's logistics datasets

export const sampleCustomers = [
  { id: 1, name: "Acme Corp", address: "10 High Street, London", postcode: "EC1A 1BB", lat: 51.5185, lng: -0.0981, quantity: 150, timeWindow: "08:00-12:00", territory: "Central" },
  { id: 2, name: "Baker & Sons", address: "25 Queens Road, Bristol", postcode: "BS1 4NT", lat: 51.4545, lng: -2.5879, quantity: 85, timeWindow: "09:00-14:00", territory: "West" },
  { id: 3, name: "Carter Industries", address: "7 Park Lane, Manchester", postcode: "M1 4BT", lat: 53.4808, lng: -2.2426, quantity: 200, timeWindow: "07:00-11:00", territory: "North" },
  { id: 4, name: "Dixon Retail", address: "42 Market Street, Birmingham", postcode: "B5 4AP", lat: 52.4862, lng: -1.8904, quantity: 120, timeWindow: "10:00-15:00", territory: "Midlands" },
  { id: 5, name: "Edwards Ltd", address: "18 Station Road, Leeds", postcode: "LS1 5DL", lat: 53.7957, lng: -1.5477, quantity: 95, timeWindow: "08:30-13:00", territory: "North" },
  { id: 6, name: "Foster & Co", address: "3 Castle Street, Edinburgh", postcode: "EH2 3AH", lat: 55.9533, lng: -3.1883, quantity: 175, timeWindow: "09:00-16:00", territory: "Scotland" },
  { id: 7, name: "Green Supplies", address: "55 Bridge Road, Cardiff", postcode: "CF10 2EE", lat: 51.4816, lng: -3.1791, quantity: 60, timeWindow: "08:00-12:00", territory: "Wales" },
  { id: 8, name: "Harris Group", address: "12 King Street, Glasgow", postcode: "G1 5QT", lat: 55.8642, lng: -4.2518, quantity: 140, timeWindow: "07:30-11:30", territory: "Scotland" },
  { id: 9, name: "Ingram Tech", address: "88 North Road, Newcastle", postcode: "NE1 7RU", lat: 54.9784, lng: -1.6174, quantity: 110, timeWindow: "09:00-14:00", territory: "North" },
  { id: 10, name: "Jackson Foods", address: "6 South Street, Brighton", postcode: "BN1 3AG", lat: 50.8225, lng: -0.1372, quantity: 90, timeWindow: "10:00-15:00", territory: "South" },
  { id: 11, name: "Knight Services", address: "29 West End, Southampton", postcode: "SO14 3HG", lat: 50.9097, lng: -1.4044, quantity: 130, timeWindow: "08:00-13:00", territory: "South" },
  { id: 12, name: "Lloyd Distribution", address: "71 East Lane, Liverpool", postcode: "L1 0AF", lat: 53.4084, lng: -2.9916, quantity: 165, timeWindow: "07:00-12:00", territory: "North West" },
  { id: 13, name: "Morris Wholesale", address: "14 Church Road, Oxford", postcode: "OX1 3LU", lat: 51.7520, lng: -1.2577, quantity: 75, timeWindow: "09:30-14:30", territory: "Central" },
  { id: 14, name: "Norton Enterprises", address: "50 River Lane, Cambridge", postcode: "CB2 1TN", lat: 52.2053, lng: 0.1218, quantity: 105, timeWindow: "08:00-13:00", territory: "East" },
  { id: 15, name: "Owen & Partners", address: "33 Hill Street, Sheffield", postcode: "S1 2GE", lat: 53.3811, lng: -1.4701, quantity: 88, timeWindow: "09:00-15:00", territory: "North" },
];

export const sampleVehicles = [
  { id: "V001", name: "Van 1", capacity: 500, startLat: 51.5074, startLng: -0.1278, startTime: "07:00", endTime: "17:00", costPerKm: 0.45, type: "Van" },
  { id: "V002", name: "Van 2", capacity: 500, startLat: 51.5074, startLng: -0.1278, startTime: "07:00", endTime: "17:00", costPerKm: 0.45, type: "Van" },
  { id: "V003", name: "Truck 1", capacity: 1200, startLat: 53.4808, startLng: -2.2426, startTime: "06:00", endTime: "18:00", costPerKm: 0.65, type: "Truck" },
  { id: "V004", name: "Truck 2", capacity: 1200, startLat: 52.4862, startLng: -1.8904, startTime: "06:00", endTime: "18:00", costPerKm: 0.65, type: "Truck" },
  { id: "V005", name: "Small Van", capacity: 250, startLat: 55.9533, startLng: -3.1883, startTime: "08:00", endTime: "16:00", costPerKm: 0.35, type: "Small Van" },
];

export const sampleRoutes = [
  {
    vehicleId: "V001", vehicleName: "Van 1", color: "#e74c3c",
    stops: [
      { customerId: 1, arrival: "08:15", departure: "08:45", load: 150 },
      { customerId: 13, arrival: "09:30", departure: "09:55", load: 75 },
      { customerId: 10, arrival: "11:20", departure: "11:50", load: 90 },
    ],
    totalDistance: 185, totalTime: "4h 35m", totalLoad: 315
  },
  {
    vehicleId: "V002", vehicleName: "Van 2", color: "#3498db",
    stops: [
      { customerId: 11, arrival: "08:30", departure: "09:00", load: 130 },
      { customerId: 7, arrival: "10:15", departure: "10:40", load: 60 },
      { customerId: 2, arrival: "11:30", departure: "12:00", load: 85 },
    ],
    totalDistance: 210, totalTime: "5h 00m", totalLoad: 275
  },
  {
    vehicleId: "V003", vehicleName: "Truck 1", color: "#2ecc71",
    stops: [
      { customerId: 3, arrival: "07:15", departure: "07:55", load: 200 },
      { customerId: 5, arrival: "08:40", departure: "09:10", load: 95 },
      { customerId: 9, arrival: "10:30", departure: "11:00", load: 110 },
      { customerId: 15, arrival: "12:15", departure: "12:45", load: 88 },
    ],
    totalDistance: 245, totalTime: "6h 45m", totalLoad: 493
  },
  {
    vehicleId: "V004", vehicleName: "Truck 2", color: "#9b59b6",
    stops: [
      { customerId: 4, arrival: "07:00", departure: "07:35", load: 120 },
      { customerId: 12, arrival: "09:10", departure: "09:45", load: 165 },
      { customerId: 14, arrival: "12:00", departure: "12:30", load: 105 },
    ],
    totalDistance: 310, totalTime: "6h 30m", totalLoad: 390
  },
  {
    vehicleId: "V005", vehicleName: "Small Van", color: "#f39c12",
    stops: [
      { customerId: 6, arrival: "08:30", departure: "09:05", load: 175 },
      { customerId: 8, arrival: "10:00", departure: "10:30", load: 140 },
    ],
    totalDistance: 75, totalTime: "3h 30m", totalLoad: 315 // note: overloaded for demo
  },
];

export const sampleTerritories = [
  { name: "Central", color: "#e74c3c", customerCount: 2, totalQuantity: 225 },
  { name: "West", color: "#3498db", customerCount: 1, totalQuantity: 85 },
  { name: "North", color: "#2ecc71", customerCount: 3, totalQuantity: 405 },
  { name: "Midlands", color: "#9b59b6", customerCount: 1, totalQuantity: 120 },
  { name: "Scotland", color: "#f39c12", customerCount: 2, totalQuantity: 315 },
  { name: "Wales", color: "#1abc9c", customerCount: 1, totalQuantity: 60 },
  { name: "South", color: "#e67e22", customerCount: 2, totalQuantity: 220 },
  { name: "North West", color: "#34495e", customerCount: 1, totalQuantity: 165 },
  { name: "East", color: "#7f8c8d", customerCount: 1, totalQuantity: 105 },
];

export const sampleScripts = [
  { id: 1, name: "Import customers", type: "import", description: "Import customer data from Excel spreadsheet" },
  { id: 2, name: "Vehicle routing", type: "optimization", description: "Run vehicle routing problem solver" },
  { id: 3, name: "Territory assignment", type: "analysis", description: "Assign customers to territories using clustering" },
  { id: 4, name: "Distance matrix", type: "calculation", description: "Calculate distance matrix between all stops" },
  { id: 5, name: "Route report", type: "report", description: "Generate PDF route report with maps" },
  { id: 6, name: "Export routes", type: "export", description: "Export route schedules to Excel" },
  { id: 7, name: "Geocode addresses", type: "geocoding", description: "Geocode customer addresses to coordinates" },
  { id: 8, name: "Gantt schedule", type: "visualization", description: "Generate Gantt chart of vehicle schedules" },
];

export const datastoreTables = [
  { name: "Customers", rows: 15, columns: 9, icon: "table" },
  { name: "Vehicles", rows: 5, columns: 8, icon: "truck" },
  { name: "Routes", rows: 5, columns: 7, icon: "route" },
  { name: "Territories", rows: 9, columns: 4, icon: "map" },
  { name: "Distance Matrix", rows: 15, columns: 15, icon: "grid" },
];
