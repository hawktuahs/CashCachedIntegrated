function App() {
  return (
    <div style={{ padding: '20px', fontFamily: 'Arial' }}>
      <h1>CashCached is Running!</h1>
      <p>If you can see this, React is working.</p>
      <p>Current time: {new Date().toLocaleString()}</p>
    </div>
  );
}

export default App;
