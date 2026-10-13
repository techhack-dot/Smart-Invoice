import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Chart as ChartJS, ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement, Title } from 'chart.js';
import { Doughnut, Bar } from 'react-chartjs-2';

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement, Title);

function App() {
  const [token, setToken] = useState(localStorage.getItem('token') || '');
  const [isRegistering, setIsRegistering] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  
  // Dashboard states
  const [expenses, setExpenses] = useState([]);
  const [title, setTitle] = useState('');
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('Software');
  const [expenseDate, setExpenseDate] = useState('');
  const [file, setFile] = useState(null);
  const [filter, setFilter] = useState('All');

  const API = "http://localhost:6969";

  const handleAuth = async (e) => {
    e.preventDefault();
    try {
      const endpoint = isRegistering ? '/api/auth/register' : '/api/auth/login';
      const res = await axios.post(`${API}${endpoint}`, { username, password });
      
      if (isRegistering) {
        alert("Registration successful! Please login.");
        setIsRegistering(false);
      } else {
        localStorage.setItem('token', res.data);
        setToken(res.data);
      }
    } catch (err) {
      alert("Error: " + (err.response?.data || "Authentication failed"));
    }
  };

  const fetchExpenses = async () => {
    try {
      const res = await axios.get(`${API}/api/expenses/user`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setExpenses(res.data);
    } catch (err) {
      console.error("Failed to load expenses", err);
    }
  };

  useEffect(() => {
    if (token) fetchExpenses();
  }, [token]);

  // ADD EXPENSE (WITH MULTIPART FILE SUPPORT)
  const handleAddExpense = async (e) => {
    e.preventDefault();
    try {
      const formData = new FormData();
      formData.append('title', title);
      formData.append('amount', parseFloat(amount));
      formData.append('category', category);
      formData.append('expenseDate', expenseDate || new Date().toISOString().split('T')[0]);
      if (file) {
        formData.append('file', file);
      }

      await axios.post(`${API}/api/expenses/add`, formData, {
        headers: { 
          Authorization: `Bearer ${token}`,
          'Content-Type': 'multipart/form-data'
        }
      });

      // Reset form
      setTitle('');
      setAmount('');
      setExpenseDate('');
      setFile(null);
      e.target.reset();
      fetchExpenses();
    } catch (err) {
      alert("Failed to save expense: " + (err.response?.data || err.message));
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Delete this expense item?")) return;
    try {
      await axios.delete(`${API}/api/expenses/${id}`);
      fetchExpenses();
    } catch (err) {
      alert("Failed to delete record.");
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken('');
  };

  // Metrics
  const totalAmount = expenses.reduce((acc, curr) => acc + curr.amount, 0);
  const filteredList = filter === 'All' ? expenses : expenses.filter(e => e.category === filter);

  // Chart Data Calculations
  const categoryTotals = expenses.reduce((acc, exp) => {
    acc[exp.category] = (acc[exp.category] || 0) + exp.amount;
    return acc;
  }, {});

  const chartData = {
    labels: Object.keys(categoryTotals),
    datasets: [
      {
        label: 'Spending by Category ($)',
        data: Object.values(categoryTotals),
        backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6'],
        borderWidth: 1,
      },
    ],
  };

  // AUTH SCREEN
  if (!token) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#0f172a', fontFamily: 'system-ui, sans-serif' }}>
        <div style={{ width: '100%', maxWidth: '420px', background: '#1e293b', padding: '35px', borderRadius: '16px', boxShadow: '0 10px 25px rgba(0,0,0,0.4)', color: '#f8fafc' }}>
          <div style={{ textAlign: 'center', marginBottom: '24px' }}>
            <div style={{ fontSize: '32px', marginBottom: '8px' }}>💳</div>
            <h2 style={{ margin: 0, fontSize: '24px', fontWeight: '700' }}>SmartInvoice</h2>
            <p style={{ color: '#94a3b8', fontSize: '14px', marginTop: '4px' }}>Enterprise Expense Automation</p>
          </div>
          <form onSubmit={handleAuth} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ fontSize: '13px', color: '#cbd5e1' }}>Username</label>
              <input type="text" placeholder="Username" value={username} onChange={e => setUsername(e.target.value)} required 
                style={{ width: '100%', padding: '12px', marginTop: '6px', background: '#334155', border: '1px solid #475569', borderRadius: '8px', color: 'white', boxSizing: 'border-box' }} />
            </div>
            <div>
              <label style={{ fontSize: '13px', color: '#cbd5e1' }}>Password</label>
              <input type="password" placeholder="••••••••" value={password} onChange={e => setPassword(e.target.value)} required 
                style={{ width: '100%', padding: '12px', marginTop: '6px', background: '#334155', border: '1px solid #475569', borderRadius: '8px', color: 'white', boxSizing: 'border-box' }} />
            </div>
            <button type="submit" style={{ padding: '12px', background: '#3b82f6', color: 'white', border: 'none', borderRadius: '8px', fontWeight: '600', cursor: 'pointer' }}>
              {isRegistering ? 'Create Account' : 'Sign In'}
            </button>
          </form>
          <p onClick={() => setIsRegistering(!isRegistering)} style={{ textAlign: 'center', fontSize: '13px', color: '#60a5fa', marginTop: '20px', cursor: 'pointer' }}>
            {isRegistering ? 'Already registered? Login here' : "Need an account? Register"}
          </p>
        </div>
      </div>
    );
  }

  // MAIN DASHBOARD SCREEN
  return (
    <div style={{ minHeight: '100vh', background: '#f8fafc', fontFamily: 'system-ui, sans-serif', color: '#1e293b' }}>
      {/* Top Navbar */}
      <nav style={{ background: '#ffffff', padding: '16px 32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #e2e8f0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <span style={{ fontSize: '24px' }}>🏢</span>
          <h2 style={{ margin: 0, fontSize: '20px', fontWeight: '700', color: '#0f172a' }}>SmartInvoice Dashboard</h2>
        </div>
        <button onClick={logout} style={{ padding: '8px 16px', background: '#ef4444', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: '600' }}>
          Logout
        </button>
      </nav>

      <div style={{ maxWidth: '1100px', margin: '30px auto', padding: '0 20px' }}>
        {/* Metric Cards Row */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px', marginBottom: '28px' }}>
          <div style={{ background: '#ffffff', padding: '24px', borderRadius: '12px', borderLeft: '5px solid #3b82f6', boxShadow: '0 2px 4px rgba(0,0,0,0.04)' }}>
            <div style={{ fontSize: '13px', fontWeight: '600', color: '#64748b' }}>TOTAL AUDITED EXPENSES</div>
            <div style={{ fontSize: '28px', fontWeight: '700', color: '#0f172a', marginTop: '8px' }}>₹{totalAmount.toFixed(2)}</div>
          </div>
          <div style={{ background: '#ffffff', padding: '24px', borderRadius: '12px', borderLeft: '5px solid #10b981', boxShadow: '0 2px 4px rgba(0,0,0,0.04)' }}>
            <div style={{ fontSize: '13px', fontWeight: '600', color: '#64748b' }}>TOTAL RECORD COUNT</div>
            <div style={{ fontSize: '28px', fontWeight: '700', color: '#0f172a', marginTop: '8px' }}>{expenses.length} Records</div>
          </div>
        </div>

        {/* Analytics Visual Chart Section */}
        {expenses.length > 0 && (
          <div style={{ background: '#ffffff', padding: '24px', borderRadius: '12px', boxShadow: '0 2px 4px rgba(0,0,0,0.04)', marginBottom: '28px', display: 'flex', gap: '20px', alignItems: 'center', justifyContent: 'space-around', flexWrap: 'wrap' }}>
            <div style={{ maxWidth: '320px', width: '100%', textAlign: 'center' }}>
              <h4 style={{ margin: '0 0 10px 0', color: '#475569' }}>Category Spending Ratio</h4>
              <Doughnut data={chartData} />
            </div>
            <div style={{ maxWidth: '450px', width: '100%', textAlign: 'center' }}>
              <h4 style={{ margin: '0 0 10px 0', color: '#475569' }}>Category Spending ($)</h4>
              <Bar data={chartData} options={{ responsive: true, plugins: { legend: { display: false } } }} />
            </div>
          </div>
        )}

        {/* Action Panel: Add Expense Form with File Attachment */}
        <div style={{ background: '#ffffff', padding: '24px', borderRadius: '12px', boxShadow: '0 2px 4px rgba(0,0,0,0.04)', marginBottom: '28px' }}>
          <h3 style={{ margin: '0 0 16px 0', fontSize: '16px', fontWeight: '600', color: '#334155' }}>Log New Expense & Attach Receipt</h3>
          <form onSubmit={handleAddExpense} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '14px', alignItems: 'end' }}>
            <div>
              <label style={{ fontSize: '12px', fontWeight: '600', color: '#64748b' }}>Description / Vendor</label>
              <input type="text" placeholder="e.g. AWS Cloud Hosting" value={title} onChange={e => setTitle(e.target.value)} required 
                style={{ width: '100%', padding: '10px', marginTop: '4px', border: '1px solid #cbd5e1', borderRadius: '6px', boxSizing: 'border-box' }} />
            </div>
            <div>
              <label style={{ fontSize: '12px', fontWeight: '600', color: '#64748b' }}>Amount (₹)</label>
              <input type="number" step="0.01" placeholder="0.00" value={amount} onChange={e => setAmount(e.target.value)} required 
                style={{ width: '100%', padding: '10px', marginTop: '4px', border: '1px solid #cbd5e1', borderRadius: '6px', boxSizing: 'border-box' }} />
            </div>
            <div>
              <label style={{ fontSize: '12px', fontWeight: '600', color: '#64748b' }}>Category</label>
              <select value={category} onChange={e => setCategory(e.target.value)} 
                style={{ width: '100%', padding: '10px', marginTop: '4px', border: '1px solid #cbd5e1', borderRadius: '6px', boxSizing: 'border-box' }}>
                <option value="Software">Software & Cloud</option>
                <option value="Travel">Corporate Travel</option>
                <option value="Office">Office Supplies</option>
                <option value="Meals">Client Meals</option>
              </select>
            </div>
            <div>
              <label style={{ fontSize: '12px', fontWeight: '600', color: '#64748b' }}>Invoice Date</label>
              <input type="date" value={expenseDate} onChange={e => setExpenseDate(e.target.value)} 
                style={{ width: '100%', padding: '10px', marginTop: '4px', border: '1px solid #cbd5e1', borderRadius: '6px', boxSizing: 'border-box' }} />
            </div>
            <div>
              <label style={{ fontSize: '12px', fontWeight: '600', color: '#64748b' }}>Attach Receipt (Image/PDF)</label>
              <input type="file" onChange={e => setFile(e.target.files[0])} 
                style={{ width: '100%', padding: '7px', marginTop: '4px', border: '1px solid #cbd5e1', borderRadius: '6px', boxSizing: 'border-box', fontSize: '12px' }} />
            </div>
            <button type="submit" style={{ padding: '11px', background: '#2563eb', color: 'white', border: 'none', borderRadius: '6px', fontWeight: '600', cursor: 'pointer' }}>
              Add Record
            </button>
          </form>
        </div>

        {/* Expense History Table */}
        <div style={{ background: '#ffffff', padding: '24px', borderRadius: '12px', boxShadow: '0 2px 4px rgba(0,0,0,0.04)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 style={{ margin: 0, fontSize: '16px', fontWeight: '600' }}>Expense Audit Ledger</h3>
            <div style={{ display: 'flex', gap: '8px' }}>
              {['All', 'Software', 'Travel', 'Office', 'Meals'].map(cat => (
                <button key={cat} onClick={() => setFilter(cat)} style={{ padding: '6px 12px', border: '1px solid #cbd5e1', borderRadius: '20px', background: filter === cat ? '#0f172a' : '#ffffff', color: filter === cat ? '#ffffff' : '#475569', fontSize: '12px', cursor: 'pointer' }}>
                  {cat}
                </button>
              ))}
            </div>
          </div>

          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#64748b', fontSize: '13px' }}>
                <th style={{ padding: '12px' }}>Description</th>
                <th style={{ padding: '12px' }}>Category</th>
                <th style={{ padding: '12px' }}>Date</th>
                <th style={{ padding: '12px' }}>Amount</th>
                <th style={{ padding: '12px' }}>Receipt Document</th>
                <th style={{ padding: '12px' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredList.map(exp => (
                <tr key={exp.id} style={{ borderBottom: '1px solid #f1f5f9', fontSize: '14px' }}>
                  <td style={{ padding: '12px', fontWeight: '500' }}>{exp.title}</td>
                  <td style={{ padding: '12px' }}>
                    <span style={{ padding: '4px 8px', borderRadius: '4px', fontSize: '12px', background: '#e0f2fe', color: '#0369a1', fontWeight: '600' }}>{exp.category}</span>
                  </td>
                  <td style={{ padding: '12px', color: '#64748b' }}>{exp.expenseDate}</td>
                  <td style={{ padding: '12px', fontWeight: '700', color: '#0f172a' }}>₹{exp.amount.toFixed(2)}</td>
                  <td style={{ padding: '12px' }}>
                    {exp.receiptFileName ? (
                      <a href={`${API}/api/expenses/receipt/${exp.receiptFileName}`} target="_blank" rel="noopener noreferrer" style={{ color: '#2563eb', fontWeight: '600', textDecoration: 'none' }}>
                        📎 View File
                      </a>
                    ) : (
                      <span style={{ color: '#94a3b8', fontSize: '12px' }}>None attached</span>
                    )}
                  </td>
                  <td style={{ padding: '12px' }}>
                    <button onClick={() => handleDelete(exp.id)} style={{ border: 'none', background: '#fee2e2', color: '#dc2626', padding: '5px 10px', borderRadius: '4px', cursor: 'pointer', fontSize: '12px', fontWeight: '600' }}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default App;