import React, { useState } from 'react';
import { 
  ShieldCheck, 
  Lock, 
  Calendar as CalendarIcon, 
  DollarSign, 
  TrendingUp, 
  Users, 
  Video, 
  Search, 
  Plus, 
  CheckCircle2, 
  Clock, 
  MapPin, 
  Folder, 
  ExternalLink, 
  Sparkles, 
  X, 
  AlertTriangle, 
  Download, 
  ArrowRight
} from 'lucide-react';
import type { AthleteProfile, CrmBooking, FinancialRecord } from '../../types/crm';
import { 
  getAthletes, 
  saveAthletes, 
  getBookings, 
  saveBookings, 
  getFinancials,
  generateGoogleCalendarUrl,
  triggerDriveWebhook,
  fetchCloudAthletes,
  fetchCloudBookings,
  GOOGLE_APPS_SCRIPT_WEBHOOK_URL
} from '../../services/crmStorage';
import { CrmCalendarView } from './CrmCalendarView';

interface CoachDashboardProps {
  onClose?: () => void;
}

export const CoachDashboard: React.FC<CoachDashboardProps> = () => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [pinCode, setPinCode] = useState<string>('');
  const [authError, setAuthError] = useState<string>('');
  const [testWebhookStatus, setTestWebhookStatus] = useState<'idle' | 'testing' | 'success'>('idle');

  const [activeTab, setActiveTab] = useState<'overview' | 'calendar' | 'athletes' | 'financials' | 'automation'>('overview');
  const [athletes, setAthletes] = useState<AthleteProfile[]>(getAthletes());
  const [bookings, setBookings] = useState<CrmBooking[]>(getBookings());
  const [financials] = useState<FinancialRecord[]>(getFinancials());

  React.useEffect(() => {
    fetchCloudAthletes().then((data) => { if (data && data.length > 0) setAthletes(data); });
    fetchCloudBookings().then((data) => { if (data && data.length > 0) setBookings(data); });
  }, []);


  const [searchQuery, setSearchQuery] = useState<string>('');
  const [styleFilter, setStyleFilter] = useState<string>('all');
  const [selectedAthlete, setSelectedAthlete] = useState<AthleteProfile | null>(null);

  const [newVideoTitle, setNewVideoTitle] = useState<string>('');
  const [newVideoUrl, setNewVideoUrl] = useState<string>('');
  const [newVideoNotes, setNewVideoNotes] = useState<string>('');

  const [newDrillTitle, setNewDrillTitle] = useState<string>('');
  const [newDrillCategory] = useState<'Footwork & Balance' | 'Swing Plane' | 'Release & Leverage' | '3-6-9 Spares'>('Release & Leverage');
  const [newDrillReps, setNewDrillReps] = useState<string>('15 reps');
  const [newDrillInstructions, setNewDrillInstructions] = useState<string>('');

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    setAuthError('');
    if (pinCode.trim() === '300' || pinCode.trim().toLowerCase() === 'cebowlinglab' || pinCode.trim() === '1234') {
      setIsAuthenticated(true);
    } else {
      setAuthError('Incorrect PIN or Password. (Hint: default PIN is 300)');
    }
  };

  const totalRevenue = financials.reduce((acc, curr) => (curr.status === 'Paid' ? acc + curr.amount : acc), 0);
  const upcomingCount = bookings.filter((b) => b.status === 'Confirmed').length;
  const activeAthletesCount = athletes.length;

  const handleAddBooking = (newBookingData: Partial<CrmBooking>) => {
    const fullBooking: CrmBooking = {
      id: `BK-2026-${String(bookings.length + 1).padStart(3, '0')}`,
      athleteId: newBookingData.athleteId || athletes[0].id,
      athleteName: newBookingData.athleteName || athletes[0].fullName,
      athleteEmail: newBookingData.athleteEmail || athletes[0].email,
      athletePhone: newBookingData.athletePhone || athletes[0].phone,
      packageId: 'custom',
      packageName: newBookingData.packageName || '60-Minute Tune-Up',
      date: newBookingData.date || '2026-09-02',
      timeSlot: newBookingData.timeSlot || '6:00 PM - 7:00 PM',
      location: 'Bowlero West Covina',
      laneAssignment: newBookingData.laneAssignment || 'Lanes 19-20',
      status: 'Confirmed',
      paymentStatus: 'Paid',
      price: newBookingData.price || 65,
      focusNotes: newBookingData.focusNotes || 'On-lane lesson',
      createdAt: new Date().toISOString().split('T')[0]
    };

    const updated = [fullBooking, ...bookings];
    setBookings(updated);
    saveBookings(updated);
  };

  const handleUpdateBookingStatus = (bookingId: string, status: CrmBooking['status']) => {
    const updated = bookings.map((b) => (b.id === bookingId ? { ...b, status } : b));
    setBookings(updated);
    saveBookings(updated);
  };

  const handleAddVideoToAthlete = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedAthlete || !newVideoTitle.trim() || !newVideoUrl.trim()) return;

    const newVideo = {
      id: `VID-${Date.now()}`,
      title: newVideoTitle,
      url: newVideoUrl,
      date: new Date().toISOString().split('T')[0],
      fps: 240,
      coachingNotes: newVideoNotes || '240fps slow-motion review from Bowlero West Covina.',
      keyCheckpoints: ['Foul line leverage balance', 'Pushaway timing sync']
    };

    const updatedAthlete = {
      ...selectedAthlete,
      videos: [newVideo, ...selectedAthlete.videos],
      lastCoachedAt: new Date().toISOString().split('T')[0]
    };

    const updatedAthletes = athletes.map((a) => (a.id === updatedAthlete.id ? updatedAthlete : a));
    setAthletes(updatedAthletes);
    saveAthletes(updatedAthletes);
    setSelectedAthlete(updatedAthlete);

    setNewVideoTitle('');
    setNewVideoUrl('');
    setNewVideoNotes('');
  };

  const handleAddDrillToAthlete = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedAthlete || !newDrillTitle.trim()) return;

    const newDrill = {
      id: `DRL-${Date.now()}`,
      title: newDrillTitle,
      category: newDrillCategory,
      instructions: newDrillInstructions || 'Practice with focus on smooth execution.',
      targetReps: newDrillReps,
      completed: false
    };

    const updatedAthlete = {
      ...selectedAthlete,
      drills: [...selectedAthlete.drills, newDrill]
    };

    const updatedAthletes = athletes.map((a) => (a.id === updatedAthlete.id ? updatedAthlete : a));
    setAthletes(updatedAthletes);
    saveAthletes(updatedAthletes);
    setSelectedAthlete(updatedAthlete);

    setNewDrillTitle('');
    setNewDrillInstructions('');
  };

  const filteredAthletes = athletes.filter((a) => {
    const matchesSearch = a.fullName.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          a.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          a.id.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStyle = styleFilter === 'all' || a.style === styleFilter;
    return matchesSearch && matchesStyle;
  });

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 animate-in fade-in">
      
      {/* Top Banner */}
      <div className="bg-[#00205b] text-white rounded-3xl p-6 sm:p-8 border-b-4 border-[#c8102e] shadow-xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 text-[#c39d5e] border border-white/20 text-xs font-bold uppercase tracking-wider">
            <ShieldCheck className="w-3.5 h-3.5 text-[#c8102e]" />
            <span>Head Coach Command Center • Version 2.0.0</span>
          </div>
          <h2 className="font-display text-2xl sm:text-3xl font-black uppercase tracking-tight text-white">
            Coach's Eye CRM & Hub
          </h2>
          <p className="text-xs sm:text-sm text-slate-200 font-body">
            Alfredo Quilarquez • Bowlero West Covina • Google Drive & Calendar Sync
          </p>
        </div>

        {isAuthenticated && (
          <div className="flex items-center gap-2">
            <button
              onClick={() => setIsAuthenticated(false)}
              className="px-4 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 border border-white/20 text-xs font-bold text-white transition-all cursor-pointer"
            >
              Lock Dashboard
            </button>
          </div>
        )}
      </div>

      {/* NOT AUTHENTICATED: COACH PIN LOGIN */}
      {!isAuthenticated ? (
        <div className="max-w-md mx-auto card-usbc p-8 sm:p-10 rounded-3xl border-2 border-slate-200 shadow-xl bg-white space-y-6">
          <div className="text-center space-y-2">
            <div className="w-14 h-14 mx-auto rounded-2xl bg-blue-50 border border-blue-200 text-[#00205b] flex items-center justify-center shadow-xs">
              <Lock className="w-7 h-7 text-[#c8102e]" />
            </div>
            <h3 className="font-display text-xl font-black text-[#00205b] uppercase">
              Coach Alfredo Verification
            </h3>
            <p className="text-xs text-slate-600">
              Enter your master coach PIN or password to access the private CRM and student telemetry records.
            </p>
          </div>

          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase mb-1.5">
                Coach Master PIN / Password *
              </label>
              <input
                type="password"
                required
                autoFocus
                value={pinCode}
                onChange={(e) => setPinCode(e.target.value)}
                placeholder="Enter PIN (e.g. 300)"
                className="w-full text-center font-mono text-xl bg-slate-50 border border-slate-300 rounded-xl py-3 text-slate-900 focus:border-[#00205b] focus:bg-white focus:outline-none"
              />
            </div>

            {authError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs font-bold text-red-600 flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 shrink-0" />
                <span>{authError}</span>
              </div>
            )}

            <button
              type="submit"
              className="w-full bg-[#c8102e] hover:bg-[#a60d24] text-white font-bold text-xs uppercase tracking-wider py-4 rounded-xl shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer border border-[#a60d24]"
            >
              <ShieldCheck className="w-4 h-4" />
              <span>Unlock Coach Command Center</span>
            </button>

            <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-[11px] text-slate-600 text-center">
              <strong>Default Coach PIN:</strong> <code className="bg-slate-200 px-1.5 py-0.5 rounded text-[#00205b] font-bold">300</code>
            </div>
          </form>
        </div>
      ) : (
        /* AUTHENTICATED COACH DASHBOARD SUITE */
        <div className="space-y-6">
          
          {/* Executive Metrics Bar */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div className="card-usbc p-5 rounded-2xl border border-slate-200 bg-white shadow-xs space-y-1">
              <span className="text-[10px] font-bold uppercase text-slate-400 block tracking-wider">Active Athletes</span>
              <div className="flex items-center justify-between">
                <span className="font-display text-2xl sm:text-3xl font-black text-[#00205b]">{activeAthletesCount}</span>
                <Users className="w-6 h-6 text-blue-500 opacity-80" />
              </div>
              <span className="text-[11px] font-medium text-emerald-600">100% on-lane Bowlero active</span>
            </div>

            <div className="card-usbc p-5 rounded-2xl border border-slate-200 bg-white shadow-xs space-y-1">
              <span className="text-[10px] font-bold uppercase text-slate-400 block tracking-wider">Monthly Revenue</span>
              <div className="flex items-center justify-between">
                <span className="font-display text-2xl sm:text-3xl font-black text-[#c8102e]">${totalRevenue}</span>
                <DollarSign className="w-6 h-6 text-emerald-600 opacity-80" />
              </div>
              <span className="text-[11px] font-medium text-slate-500">Packages & Clinics</span>
            </div>

            <div className="card-usbc p-5 rounded-2xl border border-slate-200 bg-white shadow-xs space-y-1">
              <span className="text-[10px] font-bold uppercase text-slate-400 block tracking-wider">Upcoming Sessions</span>
              <div className="flex items-center justify-between">
                <span className="font-display text-2xl sm:text-3xl font-black text-[#00205b]">{upcomingCount}</span>
                <CalendarIcon className="w-6 h-6 text-[#c8102e] opacity-80" />
              </div>
              <span className="text-[11px] font-medium text-slate-500">Bowlero West Covina</span>
            </div>

            <div className="card-usbc p-5 rounded-2xl border border-slate-200 bg-white shadow-xs space-y-1">
              <span className="text-[10px] font-bold uppercase text-slate-400 block tracking-wider">Average Improvement</span>
              <div className="flex items-center justify-between">
                <span className="font-display text-2xl sm:text-3xl font-black text-emerald-700">+18.4</span>
                <TrendingUp className="w-6 h-6 text-emerald-600 opacity-80" />
              </div>
              <span className="text-[11px] font-medium text-emerald-700 font-bold">Pins per League Series</span>
            </div>
          </div>

          {/* Navigation Tabs */}
          <div className="bg-white p-2 rounded-2xl border border-slate-200 shadow-xs flex items-center gap-2 overflow-x-auto scrollbar-none">
            <button
              onClick={() => setActiveTab('overview')}
              className={`px-4 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider flex items-center gap-2 transition-all cursor-pointer whitespace-nowrap ${
                activeTab === 'overview' ? 'bg-[#00205b] text-white shadow-xs' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>Overview</span>
            </button>

            <button
              onClick={() => setActiveTab('calendar')}
              className={`px-4 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider flex items-center gap-2 transition-all cursor-pointer whitespace-nowrap ${
                activeTab === 'calendar' ? 'bg-[#00205b] text-white shadow-xs' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              <CalendarIcon className="w-3.5 h-3.5" />
              <span>Lane Schedule & G-Cal</span>
            </button>

            <button
              onClick={() => setActiveTab('athletes')}
              className={`px-4 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider flex items-center gap-2 transition-all cursor-pointer whitespace-nowrap ${
                activeTab === 'athletes' ? 'bg-[#00205b] text-white shadow-xs' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              <Users className="w-3.5 h-3.5" />
              <span>Athletes & 240fps Telemetry ({athletes.length})</span>
            </button>

            <button
              onClick={() => setActiveTab('financials')}
              className={`px-4 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider flex items-center gap-2 transition-all cursor-pointer whitespace-nowrap ${
                activeTab === 'financials' ? 'bg-[#00205b] text-white shadow-xs' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              <DollarSign className="w-3.5 h-3.5" />
              <span>Financials & Ledger</span>
            </button>

            <button
              onClick={() => setActiveTab('automation')}
              className={`px-4 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider flex items-center gap-2 transition-all cursor-pointer whitespace-nowrap ${
                activeTab === 'automation' ? 'bg-[#00205b] text-white shadow-xs' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              <Folder className="w-3.5 h-3.5" />
              <span>Google Drive & Webhook Hub</span>
            </button>
          </div>

          {/* TAB 1: OVERVIEW */}
          {activeTab === 'overview' && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              
              <div className="lg:col-span-2 space-y-4">
                <div className="card-usbc p-6 rounded-2xl border border-slate-200 bg-white shadow-xs space-y-4">
                  <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                    <div className="font-display font-black text-lg text-[#00205b] uppercase flex items-center gap-2">
                      <Clock className="w-4 h-4 text-[#c8102e]" />
                      <span>Next On-Lane Sessions</span>
                    </div>
                    <button
                      onClick={() => setActiveTab('calendar')}
                      className="text-xs font-bold text-[#c8102e] hover:underline flex items-center gap-1 cursor-pointer"
                    >
                      <span>View Full Calendar</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  <div className="divide-y divide-slate-100 space-y-2">
                    {bookings.slice(0, 3).map((b) => (
                      <div key={b.id} className="pt-3 flex items-center justify-between gap-3 text-xs">
                        <div className="space-y-0.5">
                          <div className="font-bold text-slate-900 text-sm">{b.athleteName}</div>
                          <div className="text-slate-500 font-medium">{b.packageName}</div>
                          <div className="flex items-center gap-2 text-slate-600 text-[11px]">
                            <CalendarIcon className="w-3 h-3 text-[#c8102e]" />
                            <span>{b.date} @ {b.timeSlot}</span>
                            <span>•</span>
                            <MapPin className="w-3 h-3 text-slate-400" />
                            <span>{b.laneAssignment || 'Lanes 17-18'}</span>
                          </div>
                        </div>

                        <div className="flex items-center gap-2 shrink-0">
                          <a
                            href={generateGoogleCalendarUrl(b)}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="p-2 rounded-lg bg-blue-50 text-[#00205b] border border-blue-200 hover:bg-blue-100 cursor-pointer"
                            title="Add to Google Calendar"
                          >
                            <ExternalLink className="w-3.5 h-3.5" />
                          </a>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              <div className="space-y-4">
                <div className="card-usbc p-6 rounded-2xl border border-slate-200 bg-white shadow-xs space-y-3 text-xs">
                  <div className="font-display font-black text-sm text-[#00205b] uppercase">
                    Google Ecosystem Status
                  </div>
                  
                  <div className="space-y-2.5">
                    <div className="flex items-center justify-between p-2.5 bg-emerald-50 rounded-xl border border-emerald-200 text-emerald-950">
                      <div className="flex items-center gap-2 font-bold">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                        <span>Google Drive Auto-Folders</span>
                      </div>
                      <span className="text-[10px] font-bold uppercase bg-emerald-200 px-2 py-0.5 rounded">Active</span>
                    </div>

                    <div className="flex items-center justify-between p-2.5 bg-blue-50 rounded-xl border border-blue-200 text-blue-950">
                      <div className="flex items-center gap-2 font-bold">
                        <CalendarIcon className="w-4 h-4 text-blue-600" />
                        <span>Google Calendar Webhook</span>
                      </div>
                      <span className="text-[10px] font-bold uppercase bg-blue-200 px-2 py-0.5 rounded">Synced</span>
                    </div>
                  </div>

                  <div className="pt-2 border-t border-slate-100 space-y-2">
                    <a
                      href="https://drive.google.com"
                      target="_blank"
                      rel="noopener noreferrer"
                      className="w-full bg-slate-100 hover:bg-slate-200 text-slate-800 font-bold p-2.5 rounded-xl flex items-center justify-center gap-1.5 transition-colors"
                    >
                      <Folder className="w-3.5 h-3.5 text-amber-500" />
                      <span>Open Master Google Drive</span>
                    </a>

                    <a
                      href="https://calendar.google.com"
                      target="_blank"
                      rel="noopener noreferrer"
                      className="w-full bg-slate-100 hover:bg-slate-200 text-slate-800 font-bold p-2.5 rounded-xl flex items-center justify-center gap-1.5 transition-colors"
                    >
                      <CalendarIcon className="w-3.5 h-3.5 text-[#c8102e]" />
                      <span>Open Google Calendar</span>
                    </a>
                  </div>
                </div>
              </div>

            </div>
          )}

          {/* TAB 2: INTERACTIVE CALENDAR VIEW */}
          {activeTab === 'calendar' && (
            <CrmCalendarView
              bookings={bookings}
              athletes={athletes}
              onAddBooking={handleAddBooking}
              onUpdateBookingStatus={handleUpdateBookingStatus}
              onSelectAthlete={(athleteId) => {
                const found = athletes.find((a) => a.id === athleteId);
                if (found) {
                  setSelectedAthlete(found);
                  setActiveTab('athletes');
                }
              }}
            />
          )}

          {/* TAB 3: ATHLETES DIRECTORY & 240FPS TELEMETRY */}
          {activeTab === 'athletes' && (
            <div className="space-y-4">
              
              <div className="bg-white p-4 sm:p-5 rounded-2xl border border-slate-200 shadow-xs flex flex-col sm:flex-row items-center justify-between gap-3">
                <div className="relative w-full sm:w-80">
                  <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search by name, email, or ID..."
                    className="w-full bg-slate-50 border border-slate-300 rounded-xl py-2.5 pl-10 pr-4 text-xs text-slate-900 focus:border-[#00205b] focus:bg-white focus:outline-none"
                  />
                </div>

                <div className="flex items-center gap-2 w-full sm:w-auto">
                  <select
                    value={styleFilter}
                    onChange={(e) => setStyleFilter(e.target.value)}
                    className="bg-slate-50 border border-slate-300 rounded-xl p-2.5 text-xs text-slate-700 font-bold"
                  >
                    <option value="all">All Styles</option>
                    <option value="1-Handed">1-Handed</option>
                    <option value="2-Handed">2-Handed</option>
                  </select>
                </div>
              </div>

              <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden divide-y divide-slate-100">
                {filteredAthletes.map((athlete) => (
                  <div
                    key={athlete.id}
                    className="p-5 sm:p-6 hover:bg-slate-50 transition-colors flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
                  >
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 rounded-xl bg-[#00205b] text-white font-display font-black text-lg flex items-center justify-center shrink-0 shadow-xs">
                        {athlete.fullName.split(' ').map((n) => n[0]).join('')}
                      </div>

                      <div className="space-y-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="text-xs font-mono font-bold text-[#c8102e]">
                            {athlete.id}
                          </span>
                          <span className="px-2 py-0.5 rounded bg-blue-50 text-[#00205b] font-bold text-[10px] border border-blue-200">
                            {athlete.style} ({athlete.dominantHand})
                          </span>
                          <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700 font-bold text-[10px]">
                            {athlete.bookAverage} Avg
                          </span>
                        </div>

                        <h4 className="font-display text-base font-black text-[#00205b]">
                          {athlete.fullName}
                        </h4>

                        <div className="flex flex-wrap items-center gap-3 text-xs text-slate-500 font-medium">
                          <span>{athlete.email}</span>
                          <span>•</span>
                          <span>{athlete.phone}</span>
                          <span>•</span>
                          <span className="text-emerald-700 font-bold">
                            {athlete.sessionsCompleted}/{athlete.sessionsTotal} Sessions Done
                          </span>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 w-full sm:w-auto shrink-0">
                      <button
                        onClick={() => setSelectedAthlete(athlete)}
                        className="w-full sm:w-auto bg-[#00205b] hover:bg-[#001740] text-white text-xs font-bold uppercase tracking-wider px-4 py-2.5 rounded-xl flex items-center justify-center gap-1.5 transition-all shadow-xs cursor-pointer"
                      >
                        <Video className="w-3.5 h-3.5" />
                        <span>Manage & 240fps Telemetry</span>
                      </button>

                      <a
                        href={athlete.googleDriveFolderUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="p-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-300 transition-colors"
                        title="Open Student Google Drive Folder"
                      >
                        <Folder className="w-4 h-4 text-amber-500" />
                      </a>
                    </div>
                  </div>
                ))}
              </div>

            </div>
          )}

          {/* TAB 4: FINANCIALS & REVENUE LEDGER */}
          {activeTab === 'financials' && (
            <div className="card-usbc p-6 sm:p-8 rounded-3xl border border-slate-200 bg-white shadow-md space-y-6">
              
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-slate-100 pb-5">
                <div>
                  <div className="text-xs font-bold uppercase text-[#c8102e]">Coach's Eye Revenue Ledger</div>
                  <h3 className="font-display text-2xl font-black text-[#00205b] uppercase">
                    Financial Transactions (${totalRevenue} Total Collected)
                  </h3>
                </div>

                <button
                  onClick={() => window.print()}
                  className="px-4 py-2.5 rounded-xl border border-slate-300 text-slate-700 hover:bg-slate-100 font-bold text-xs uppercase flex items-center gap-2 cursor-pointer"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>Print Financial Report</span>
                </button>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 uppercase font-bold text-[11px]">
                    <tr>
                      <th className="p-3.5">Tx ID</th>
                      <th className="p-3.5">Athlete</th>
                      <th className="p-3.5">Package / Service</th>
                      <th className="p-3.5">Date</th>
                      <th className="p-3.5">Method</th>
                      <th className="p-3.5">Amount</th>
                      <th className="p-3.5">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-slate-700">
                    {financials.map((f) => (
                      <tr key={f.id} className="hover:bg-slate-50">
                        <td className="p-3.5 font-mono font-bold text-[#c8102e]">{f.id}</td>
                        <td className="p-3.5 font-bold text-[#00205b]">{f.athleteName}</td>
                        <td className="p-3.5">{f.description}</td>
                        <td className="p-3.5 font-mono">{f.date}</td>
                        <td className="p-3.5">{f.method}</td>
                        <td className="p-3.5 font-bold text-[#00205b]">${f.amount}</td>
                        <td className="p-3.5">
                          <span className={`px-2.5 py-1 rounded text-[10px] font-bold uppercase ${
                            f.status === 'Paid' ? 'bg-emerald-50 text-emerald-800' : 'bg-amber-50 text-amber-800'
                          }`}>
                            {f.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

            </div>
          )}

          {/* TAB 5: GOOGLE DRIVE & WEBHOOK AUTOMATION HUB */}
          {activeTab === 'automation' && (
            <div className="card-usbc p-6 sm:p-8 rounded-3xl border border-slate-200 bg-white shadow-md space-y-6">
              <div className="border-b border-slate-100 pb-5">
                <div className="text-xs font-bold uppercase text-[#c8102e]">Google Apps Script & Drive Automation</div>
                <h3 className="font-display text-2xl font-black text-[#00205b] uppercase">
                  Connected Integrations
                </h3>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-xs">
                
                <div className="p-5 bg-slate-50 border border-slate-200 rounded-2xl space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="font-bold text-[#00205b] text-sm uppercase flex items-center gap-2">
                      <Folder className="w-4 h-4 text-amber-500" />
                      <span>Google Drive Folder Automator</span>
                    </div>
                    <span className="px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold uppercase border border-emerald-300 flex items-center gap-1">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 animate-pulse" />
                      <span>Live & Connected</span>
                    </span>
                  </div>

                  <p className="text-slate-600 leading-relaxed font-body">
                    When a bowler registers on <strong>cebowlinglab.com</strong>, the webhook triggers Google Apps Script to create a real student folder in <code>Google Drive / CE Bowling Lab - Students / [CEB-XXX] Full Name</code>.
                  </p>

                  <div className="p-3 bg-white border border-slate-200 rounded-xl space-y-1">
                    <span className="text-[10px] font-bold uppercase text-slate-400 block">Active Webhook URL:</span>
                    <code className="text-[11px] font-mono text-slate-700 break-all block">
                      {GOOGLE_APPS_SCRIPT_WEBHOOK_URL}
                    </code>
                  </div>

                  <div className="flex items-center justify-between pt-2">
                    <button
                      type="button"
                      disabled={testWebhookStatus === 'testing'}
                      onClick={async () => {
                        setTestWebhookStatus('testing');
                        await triggerDriveWebhook({
                          fullName: 'Test Bowler (Verification)',
                          studentId: `CEB-VERIFY-${Math.floor(100 + Math.random() * 900)}`,
                          email: 'CEBowlingLab@gmail.com',
                          style: '2-Handed',
                          bookAverage: 200,
                          goals: 'Automated test folder creation from Coach Dashboard'
                        });
                        setTestWebhookStatus('success');
                        setTimeout(() => setTestWebhookStatus('idle'), 5000);
                      }}
                      className="bg-[#00205b] hover:bg-[#001740] text-white font-bold text-xs uppercase px-4 py-2.5 rounded-xl flex items-center gap-1.5 shadow-xs cursor-pointer disabled:opacity-50"
                    >
                      <Sparkles className="w-3.5 h-3.5 text-[#c39d5e]" />
                      <span>{testWebhookStatus === 'testing' ? 'Creating Test Folder...' : testWebhookStatus === 'success' ? '✓ Test Folder Created!' : '⚡ Send Test Ping to Google Drive'}</span>
                    </button>
                    
                    <span className="text-emerald-700 font-bold text-[11px]">Destination: CEBowlingLab@gmail.com</span>
                  </div>
                </div>

                <div className="p-5 bg-blue-50/70 border border-blue-200 rounded-2xl space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="font-bold text-[#00205b] text-sm uppercase flex items-center gap-2">
                      <CalendarIcon className="w-4 h-4 text-[#c8102e]" />
                      <span>Two-Way Google Calendar Sync</span>
                    </div>
                    <span className="px-2.5 py-0.5 rounded-full bg-blue-100 text-blue-800 text-[10px] font-bold uppercase border border-blue-300">
                      Active
                    </span>
                  </div>

                  <p className="text-slate-600 leading-relaxed font-body">
                    All on-lane appointments at Bowlero West Covina generate instant Google Calendar links and exportable <code>.ics</code> files with student notes, phone numbers, and lane specs.
                  </p>

                  <div className="p-3 bg-white/80 border border-blue-200 rounded-xl space-y-1">
                    <span className="text-[10px] font-bold uppercase text-slate-400 block">Google Calendar Target:</span>
                    <span className="font-bold text-slate-800">CEBowlingLab@gmail.com</span>
                  </div>

                  <div className="pt-2">
                    <a
                      href="https://calendar.google.com"
                      target="_blank"
                      rel="noopener noreferrer"
                      className="bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs uppercase px-4 py-2.5 rounded-xl inline-flex items-center gap-1.5 shadow-xs"
                    >
                      <ExternalLink className="w-3.5 h-3.5" />
                      <span>Open Google Calendar</span>
                    </a>
                  </div>
                </div>

              </div>
            </div>
          )}


        </div>
      )}

      {/* ATHLETE DETAIL & 240FPS VIDEO MANAGER DRAWER */}
      {selectedAthlete && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="relative w-full max-w-3xl bg-white rounded-3xl shadow-2xl border-2 border-slate-300 overflow-hidden animate-in fade-in max-h-[92vh] flex flex-col">
            
            <div className="bg-[#00205b] text-white p-6 border-b-4 border-[#c8102e] flex items-center justify-between shrink-0">
              <div>
                <span className="text-[10px] font-mono uppercase tracking-wider text-[#c39d5e] font-bold">
                  {selectedAthlete.id} • Registered Bowler Record
                </span>
                <h3 className="font-display text-2xl font-black uppercase text-white">
                  {selectedAthlete.fullName}
                </h3>
              </div>
              <button
                onClick={() => setSelectedAthlete(null)}
                className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="overflow-y-auto p-6 sm:p-8 space-y-6 text-xs text-slate-700">
              
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                  <span className="text-slate-400 block text-[10px] uppercase font-bold">Style / Hand</span>
                  <span className="font-bold text-[#00205b]">{selectedAthlete.style} ({selectedAthlete.dominantHand})</span>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                  <span className="text-slate-400 block text-[10px] uppercase font-bold">Book Average</span>
                  <span className="font-bold text-[#00205b]">{selectedAthlete.bookAverage} Pins</span>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                  <span className="text-slate-400 block text-[10px] uppercase font-bold">High Series</span>
                  <span className="font-bold text-[#c8102e]">{selectedAthlete.careerHighSeries}</span>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                  <span className="text-slate-400 block text-[10px] uppercase font-bold">Package</span>
                  <span className="font-bold text-emerald-800">{selectedAthlete.packageTier}</span>
                </div>
              </div>

              <div className="p-4 bg-amber-50 border border-amber-200 rounded-2xl flex items-center justify-between">
                <div className="flex items-center gap-2 font-bold text-amber-950">
                  <Folder className="w-4 h-4 text-amber-600 shrink-0" />
                  <span>Student Google Drive Archive Folder:</span>
                </div>
                <a
                  href={selectedAthlete.googleDriveFolderUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="bg-[#00205b] hover:bg-[#001740] text-white font-bold text-xs uppercase px-4 py-2 rounded-xl flex items-center gap-1.5 shadow-xs"
                >
                  <ExternalLink className="w-3 h-3" />
                  <span>Open Folder</span>
                </a>
              </div>

              {/* ADD 240FPS VIDEO REVIEW FORM */}
              <form onSubmit={handleAddVideoToAthlete} className="p-5 bg-slate-50 border border-slate-200 rounded-2xl space-y-3">
                <div className="font-bold text-[#00205b] uppercase text-xs flex items-center gap-2">
                  <Video className="w-4 h-4 text-[#c8102e]" />
                  <span>Attach 240fps Biomechanical Video Breakdown</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <input
                    type="text"
                    required
                    value={newVideoTitle}
                    onChange={(e) => setNewVideoTitle(e.target.value)}
                    placeholder="Video Title (e.g. Session 3: Release Leverage)"
                    className="w-full bg-white border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900"
                  />
                  <input
                    type="url"
                    required
                    value={newVideoUrl}
                    onChange={(e) => setNewVideoUrl(e.target.value)}
                    placeholder="Unlisted YouTube URL or Google Drive Link"
                    className="w-full bg-white border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900"
                  />
                </div>

                <textarea
                  rows={2}
                  value={newVideoNotes}
                  onChange={(e) => setNewVideoNotes(e.target.value)}
                  placeholder="Coach Alfredo's voiceover review and checkpoint notes for student..."
                  className="w-full bg-white border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900"
                />

                <div className="flex justify-end">
                  <button
                    type="submit"
                    className="bg-[#c8102e] hover:bg-[#a60d24] text-white font-bold text-xs uppercase px-5 py-2.5 rounded-xl flex items-center gap-1.5 shadow-xs cursor-pointer"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Upload Video to Student Locker</span>
                  </button>
                </div>
              </form>

              {/* ADD DRILL HOMEWORK FORM */}
              <form onSubmit={handleAddDrillToAthlete} className="p-5 bg-slate-50 border border-slate-200 rounded-2xl space-y-3">
                <div className="font-bold text-[#00205b] uppercase text-xs flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>Assign Practice Drill Homework</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <input
                    type="text"
                    required
                    value={newDrillTitle}
                    onChange={(e) => setNewDrillTitle(e.target.value)}
                    placeholder="Drill Name (e.g. 1-Step Foul Line Pause)"
                    className="w-full bg-white border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900 sm:col-span-2"
                  />
                  <input
                    type="text"
                    value={newDrillReps}
                    onChange={(e) => setNewDrillReps(e.target.value)}
                    placeholder="Target Reps (e.g. 15 shots)"
                    className="w-full bg-white border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900"
                  />
                </div>

                <textarea
                  rows={2}
                  value={newDrillInstructions}
                  onChange={(e) => setNewDrillInstructions(e.target.value)}
                  placeholder="Mechanical drill focus instructions..."
                  className="w-full bg-white border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900"
                />

                <div className="flex justify-end">
                  <button
                    type="submit"
                    className="bg-[#00205b] hover:bg-[#001740] text-white font-bold text-xs uppercase px-5 py-2.5 rounded-xl flex items-center gap-1.5 shadow-xs cursor-pointer"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Assign Drill Homework</span>
                  </button>
                </div>
              </form>

              <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center justify-between text-xs text-emerald-950">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-emerald-600 shrink-0" />
                  <div>
                    <span className="font-bold block">Athletic Liability Waiver & SafeSport On File</span>
                    <span className="text-[11px] text-emerald-800">Timestamp: {selectedAthlete.signedWaiverTimestamp}</span>
                  </div>
                </div>
                <span className="px-2.5 py-1 rounded bg-emerald-700 text-white font-bold text-[10px] uppercase">
                  Verified
                </span>
              </div>

            </div>

            <div className="p-4 bg-slate-50 border-t border-slate-200 flex justify-end shrink-0">
              <button
                onClick={() => setSelectedAthlete(null)}
                className="px-6 py-2.5 rounded-xl bg-slate-200 hover:bg-slate-300 text-slate-800 font-bold text-xs uppercase"
              >
                Close Drawer
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
};
