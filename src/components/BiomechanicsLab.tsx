import React, { useState } from 'react';
import { 
  Sparkles, 
  FileSpreadsheet
} from 'lucide-react';

export const BiomechanicsLab: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'spares' | 'oil-guide' | 'mechanics'>('spares');

  // Spare System State
  const [selectedPin, setSelectedPin] = useState<number>(10);

  // Oil Guide Selection
  const [selectedPatternView, setSelectedPatternView] = useState<'house' | 'sport'>('house');

  // 3-6-9 Spare Rules Mapping for Right-Handed bowler
  const spareRules: Record<number, { boards: string; target: string; ballType: string; tip: string }> = {
    7: {
      boards: "Move feet 9 boards RIGHT (or 11 for flat corner angle)",
      target: "Look at 3rd Arrow (Board 15)",
      ballType: "Plastic / Polyester Ball (Zero Hook)",
      tip: "Walk toward your target line; keep shoulder open toward the left corner of the pin deck."
    },
    4: {
      boards: "Move feet 6 boards RIGHT",
      target: "Look at 3rd Arrow (Board 15)",
      ballType: "Plastic / Spare Ball",
      tip: "Square shoulders directly to the 4-pin. Let swing trace cleanly through the 3rd arrow."
    },
    2: {
      boards: "Move feet 3 boards RIGHT",
      target: "Look at 2nd Arrow (Board 10)",
      ballType: "Plastic / Spare Ball",
      tip: "Standard 3-board parallel shift right from your normal strike stance."
    },
    10: {
      boards: "Move feet 9 to 12 boards LEFT",
      target: "Look across lane at 3rd Arrow / Board 15-17",
      ballType: "Polyester / Plastic Ball thrown flat",
      tip: "The corner pin killer. Flatten wrist completely, drift 2 boards right, and throw straight across the high oil in the middle."
    },
    6: {
      boards: "Move feet 6 boards LEFT",
      target: "Look across lane at 3rd Arrow (Board 15)",
      ballType: "Plastic / Minimal Hook",
      tip: "Cross-lane trajectory takes lane friction and oil breakdown out of play."
    },
    3: {
      boards: "Move feet 3 boards LEFT",
      target: "Look at 3rd Arrow (Board 15)",
      ballType: "Plastic or Urethane with flattened axis",
      tip: "Crosses middle lane oil directly into 3-6 pocket zone."
    },
    8: {
      boards: "Standard Strike Stance (0 Board shift)",
      target: "Strike Target (Board 10-12)",
      ballType: "Strike Ball or Plastic",
      tip: "Directly behind headpin. Treat with solid strike line or straight spare ball."
    },
    9: {
      boards: "Move feet 2 boards LEFT",
      target: "Strike Target (Board 11)",
      ballType: "Plastic / Straight release",
      tip: "Do not over-hook past the 9-pin; roll firmly through the 3-pin vacancy."
    },
    1: {
      boards: "Standard Strike Stance (0 Board shift)",
      target: "2nd Arrow (Board 10)",
      ballType: "Primary Strike Ball",
      tip: "Standard headpin pocket line with continuous roll."
    },
    5: {
      boards: "Standard Strike Stance (0 Board shift)",
      target: "2nd Arrow (Board 10)",
      ballType: "Plastic or Strike Ball",
      tip: "Center pin conversion with flat follow-through."
    }
  };

  const currentSpare = spareRules[selectedPin] || spareRules[10];

  return (
    <div className="space-y-12">
      
      {/* Tab Navigation Selector with Roomy Spacing & Alignment */}
      <div className="flex flex-wrap items-center justify-center gap-3 sm:gap-4">
        <button
          onClick={() => setActiveTab('spares')}
          className={`px-7 py-3.5 rounded-xl text-xs sm:text-sm font-bold uppercase tracking-wider transition-all cursor-pointer ${
            activeTab === 'spares'
              ? 'bg-[#00205b] text-white shadow-md'
              : 'bg-white text-slate-700 hover:bg-slate-100 border border-slate-300'
          }`}
        >
          1. 3-6-9 Spare Matrix System
        </button>

        <button
          onClick={() => setActiveTab('oil-guide')}
          className={`px-7 py-3.5 rounded-xl text-xs sm:text-sm font-bold uppercase tracking-wider transition-all cursor-pointer ${
            activeTab === 'oil-guide'
              ? 'bg-[#c8102e] text-white shadow-md'
              : 'bg-white text-slate-700 hover:bg-slate-100 border border-slate-300'
          }`}
        >
          2. Lane Transition & Oil Pattern Guide
        </button>

        <button
          onClick={() => setActiveTab('mechanics')}
          className={`px-7 py-3.5 rounded-xl text-xs sm:text-sm font-bold uppercase tracking-wider transition-all cursor-pointer ${
            activeTab === 'mechanics'
              ? 'bg-[#00205b] text-white shadow-md'
              : 'bg-white text-slate-700 hover:bg-slate-100 border border-slate-300'
          }`}
        >
          3. 1-Hand vs 2-Hand Telemetry
        </button>
      </div>

      {/* TAB 1: 3-6-9 SPARE SYSTEM CALCULATOR */}
      {activeTab === 'spares' && (
        <div className="card-usbc p-8 sm:p-12 border border-slate-200 shadow-md">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            
            {/* Left: Clean USBC Pin Deck Selector */}
            <div className="lg:col-span-5 text-center space-y-6">
              <div>
                <div className="text-xs font-bold text-[#00205b] uppercase tracking-wider mb-1">
                  Interactive Pin Deck
                </div>
                <p className="text-xs text-slate-500">
                  Click any pin leaf below to calculate exact foot & target board adjustments:
                </p>
              </div>

              {/* Pin Formation with generous padding & hover */}
              <div className="inline-block p-8 bg-slate-100 rounded-3xl border border-slate-300 shadow-inner">
                {/* Row 4: 7, 8, 9, 10 */}
                <div className="flex justify-center gap-4 mb-3.5">
                  {[7, 8, 9, 10].map((pin) => (
                    <button
                      key={pin}
                      onClick={() => setSelectedPin(pin)}
                      className={`w-13 h-13 rounded-full font-display font-black text-base flex items-center justify-center transition-all cursor-pointer ${
                        selectedPin === pin
                          ? 'bg-[#c8102e] text-white ring-4 ring-red-200 shadow-md scale-105'
                          : 'bg-white text-slate-800 hover:bg-slate-50 border border-slate-300'
                      }`}
                    >
                      {pin}
                    </button>
                  ))}
                </div>

                {/* Row 3: 4, 5, 6 */}
                <div className="flex justify-center gap-6 mb-3.5">
                  {[4, 5, 6].map((pin) => (
                    <button
                      key={pin}
                      onClick={() => setSelectedPin(pin)}
                      className={`w-13 h-13 rounded-full font-display font-black text-base flex items-center justify-center transition-all cursor-pointer ${
                        selectedPin === pin
                          ? 'bg-[#c8102e] text-white ring-4 ring-red-200 shadow-md scale-105'
                          : 'bg-white text-slate-800 hover:bg-slate-50 border border-slate-300'
                      }`}
                    >
                      {pin}
                    </button>
                  ))}
                </div>

                {/* Row 2: 2, 3 */}
                <div className="flex justify-center gap-8 mb-3.5">
                  {[2, 3].map((pin) => (
                    <button
                      key={pin}
                      onClick={() => setSelectedPin(pin)}
                      className={`w-13 h-13 rounded-full font-display font-black text-base flex items-center justify-center transition-all cursor-pointer ${
                        selectedPin === pin
                          ? 'bg-[#c8102e] text-white ring-4 ring-red-200 shadow-md scale-105'
                          : 'bg-white text-slate-800 hover:bg-slate-50 border border-slate-300'
                      }`}
                    >
                      {pin}
                    </button>
                  ))}
                </div>

                {/* Row 1: 1 (Headpin) */}
                <div className="flex justify-center">
                  <button
                    onClick={() => setSelectedPin(1)}
                    className={`w-13 h-13 rounded-full font-display font-black text-base flex items-center justify-center transition-all cursor-pointer ${
                      selectedPin === 1
                        ? 'bg-[#c8102e] text-white ring-4 ring-red-200 shadow-md scale-105'
                        : 'bg-white text-slate-800 hover:bg-slate-50 border border-slate-300'
                    }`}
                  >
                    1
                  </button>
                </div>
              </div>

              <div className="text-xs font-bold text-slate-600">
                Selected Target: <span className="text-[#c8102e] font-extrabold">#{selectedPin} Pin</span> (Right-Handed System)
              </div>
            </div>

            {/* Right: Calculated Spare Prescription */}
            <div className="lg:col-span-7 space-y-6 text-left">
              <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-md bg-blue-50 border border-blue-200 text-[#00205b] text-xs font-bold uppercase tracking-wider">
                <span>USBC 3-6-9 Mathematical Spare Formula</span>
              </div>

              <h3 className="font-display text-2xl sm:text-3xl font-black text-[#00205b] uppercase">
                Conversion Prescription for #{selectedPin} Pin
              </h3>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                
                {/* Feet Shift */}
                <div className="bg-slate-50 p-5 rounded-2xl border border-slate-200">
                  <div className="text-xs font-bold text-slate-500 uppercase">Stance Adjustment</div>
                  <div className="text-base font-display font-bold text-[#00205b] mt-1.5">
                    {currentSpare.boards}
                  </div>
                </div>

                {/* Target Arrow */}
                <div className="bg-slate-50 p-5 rounded-2xl border border-slate-200">
                  <div className="text-xs font-bold text-slate-500 uppercase">Lane Target / Visual Focal Point</div>
                  <div className="text-base font-display font-bold text-[#c8102e] mt-1.5">
                    {currentSpare.target}
                  </div>
                </div>

                {/* Recommended Ball */}
                <div className="bg-slate-50 p-5 rounded-2xl border border-slate-200 sm:col-span-2">
                  <div className="text-xs font-bold text-slate-500 uppercase">Recommended Ball & Surface</div>
                  <div className="text-sm font-display font-bold text-slate-800 mt-1.5">
                    {currentSpare.ballType}
                  </div>
                </div>
              </div>

              {/* Coach's Pro Tip */}
              <div className="bg-amber-50 p-5 rounded-2xl border border-amber-200">
                <div className="text-xs font-bold text-amber-900 uppercase mb-1.5 flex items-center gap-1.5">
                  <Sparkles className="w-4 h-4 text-amber-600" />
                  <span>Coach Alfredo's Execution Note</span>
                </div>
                <p className="text-xs sm:text-sm text-slate-800 leading-relaxed font-body">
                  {currentSpare.tip}
                </p>
              </div>

            </div>

          </div>
        </div>
      )}

      {/* TAB 2: AUTHENTIC OIL PATTERN & TRANSITION REFERENCE GUIDE */}
      {activeTab === 'oil-guide' && (
        <div className="space-y-10">
          
          {/* Top Explainer */}
          <div className="card-usbc p-8 sm:p-10 border border-slate-200">
            <div className="max-w-3xl space-y-3">
              <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded bg-red-50 text-[#c8102e] text-xs font-bold uppercase">
                <FileSpreadsheet className="w-4 h-4" />
                <span>Real-World Lane Play & Friction Management</span>
              </div>
              <h3 className="font-display text-2xl sm:text-3xl font-black text-[#00205b] uppercase">
                How Oil Depletes & Transitions Across 3 Blocks
              </h3>
              <p className="text-slate-600 text-sm leading-relaxed pt-1">
                Bowling balls do not roll on dry wood—they interact with dynamic conditioner patterns (typically 20-30mL of mineral oil). Understanding the physical 2D cross-section and the 3-stage breakdown allows you to make decisive ball and foot adjustments before losing pin carry.
              </p>
            </div>

            {/* Pattern Type Toggle */}
            <div className="mt-8 flex flex-wrap gap-3">
              <button
                onClick={() => setSelectedPatternView('house')}
                className={`px-6 py-3 rounded-xl text-xs font-bold uppercase tracking-wider border transition-all cursor-pointer ${
                  selectedPatternView === 'house'
                    ? 'bg-[#00205b] text-white border-[#00205b] shadow-xs'
                    : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-50'
                }`}
              >
                Typical House Shot (42 Ft - 10:1 Ratio)
              </button>
              <button
                onClick={() => setSelectedPatternView('sport')}
                className={`px-6 py-3 rounded-xl text-xs font-bold uppercase tracking-wider border transition-all cursor-pointer ${
                  selectedPatternView === 'sport'
                    ? 'bg-[#c8102e] text-white border-[#c8102e] shadow-xs'
                    : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-50'
                }`}
              >
                PBA / USBC Sport Pattern (39 Ft - 2.5:1 Flat)
              </button>
            </div>
          </div>

          {/* Authentic 2D Oil Cross Section Chart Diagram with Roomy Layout */}
          <div className="card-usbc p-8 sm:p-10 border border-slate-200">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-5 mb-8">
              <div>
                <h4 className="font-display text-xl font-bold text-[#00205b] uppercase">
                  {selectedPatternView === 'house' ? 'Typical House Pattern Cross-Section' : 'USBC / PBA Sport Flat Cross-Section'}
                </h4>
                <p className="text-xs text-slate-500 mt-1">
                  Oil volume distribution (Units of Oil vs Boards 1 to 39 from left gutter to right gutter)
                </p>
              </div>
              <span className="text-xs font-bold px-3.5 py-1.5 rounded-full bg-blue-50 text-[#00205b] border border-blue-200 self-start sm:self-auto">
                {selectedPatternView === 'house' ? 'High Forgiveness Crown' : 'Flat Tournament Difficulty'}
              </span>
            </div>

            {/* SVG 2D Cross Section Graph */}
            <div className="bg-slate-900 rounded-2xl p-6 sm:p-8 text-white overflow-hidden relative shadow-inner">
              <div className="text-[11px] font-mono text-slate-400 mb-3 flex justify-between">
                <span>BOARD 1 (LEFT GUTTER)</span>
                <span className="text-amber-400 font-bold">BOARD 20 (HEADPIN CENTER)</span>
                <span>BOARD 39 (RIGHT GUTTER)</span>
              </div>

              <svg className="w-full h-52 sm:h-64" viewBox="0 0 500 180">
                <defs>
                  <linearGradient id="oilGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                    <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.9" />
                    <stop offset="100%" stopColor="#0284c7" stopOpacity="0.4" />
                  </linearGradient>
                  <linearGradient id="sportGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                    <stop offset="0%" stopColor="#f87171" stopOpacity="0.9" />
                    <stop offset="100%" stopColor="#dc2626" stopOpacity="0.4" />
                  </linearGradient>
                </defs>

                {/* Gridlines */}
                <line x1="20" y1="150" x2="480" y2="150" stroke="#334155" strokeWidth="1.5" />
                <line x1="20" y1="100" x2="480" y2="100" stroke="#1e293b" strokeWidth="1" strokeDasharray="4 4" />
                <line x1="20" y1="50" x2="480" y2="50" stroke="#1e293b" strokeWidth="1" strokeDasharray="4 4" />
                <line x1="250" y1="20" x2="250" y2="150" stroke="#64748b" strokeWidth="1" strokeDasharray="2 2" />

                {/* Oil Volume Shape */}
                {selectedPatternView === 'house' ? (
                  <>
                    {/* Crown House Shot Shape */}
                    <path
                      d="M 30 150 L 100 145 Q 160 140 180 60 Q 250 30 320 60 Q 340 140 400 145 L 470 150 Z"
                      fill="url(#oilGradient)"
                      stroke="#38bdf8"
                      strokeWidth="2.5"
                    />
                    <text x="250" y="25" fill="#fde047" fontSize="11" fontWeight="bold" textAnchor="middle" fontFamily="sans-serif">
                      HEAVY OIL CROWN (80+ UNITS)
                    </text>
                    <text x="70" y="135" fill="#94a3b8" fontSize="9" fontFamily="sans-serif">
                      DRY OUTSIDE (5 UNITS)
                    </text>
                    <text x="430" y="135" fill="#94a3b8" fontSize="9" fontFamily="sans-serif">
                      DRY OUTSIDE
                    </text>
                  </>
                ) : (
                  <>
                    {/* Flat Sport Shot Shape */}
                    <path
                      d="M 30 150 L 50 75 L 450 75 L 470 150 Z"
                      fill="url(#sportGradient)"
                      stroke="#f87171"
                      strokeWidth="2.5"
                    />
                    <text x="250" y="65" fill="#fca5a5" fontSize="11" fontWeight="bold" textAnchor="middle" fontFamily="sans-serif">
                      FLAT 2.5:1 RATIO ACROSS ALL BOARDS (NO FREE HOOK)
                    </text>
                  </>
                )}
              </svg>

              <div className="mt-4 flex items-center justify-between text-xs text-slate-400 font-mono">
                <span>0 FT (Foul Line Release)</span>
                <span>Buffer Distance: {selectedPatternView === 'house' ? '42 Feet Length' : '39 Feet Length'}</span>
                <span>60 FT (Pin Deck)</span>
              </div>
            </div>
          </div>

          {/* 3-Stage Transition Breakdown Cards with Equal Height & Line Up */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 items-stretch">
            
            {/* Stage 1 */}
            <div className="card-usbc p-8 border-t-4 border-t-emerald-600 flex flex-col justify-between h-full shadow-sm">
              <div>
                <div className="flex items-center justify-between mb-4">
                  <span className="px-3 py-1 rounded-full bg-emerald-50 text-emerald-800 text-[10px] font-bold uppercase">
                    Game 1: The Fresh
                  </span>
                  <span className="text-xs text-slate-400 font-bold">0-40 Shots</span>
                </div>
                <h5 className="font-display text-lg sm:text-xl font-bold text-[#00205b] mb-2.5">
                  1. High Friction Track Baseline
                </h5>
                <p className="text-slate-600 text-xs sm:text-sm leading-relaxed mb-6">
                  Conditioner is pristine. Outer boards have high friction, while middle boards provide hold.
                </p>
              </div>
              <div className="space-y-2 text-xs bg-slate-50 p-4 rounded-xl border border-slate-200">
                <div className="font-bold text-slate-800">Recommended Move:</div>
                <div className="text-slate-600">• Play Track Area (Board 10-12)</div>
                <div className="text-slate-600">• Solid Symmetrical / 2000 Grit</div>
              </div>
            </div>

            {/* Stage 2 */}
            <div className="card-usbc p-8 border-t-4 border-t-amber-500 flex flex-col justify-between h-full shadow-md">
              <div>
                <div className="flex items-center justify-between mb-4">
                  <span className="px-3 py-1 rounded-full bg-amber-50 text-amber-800 text-[10px] font-bold uppercase">
                    Games 2-3: Transition
                  </span>
                  <span className="text-xs text-slate-400 font-bold">40-90 Shots</span>
                </div>
                <h5 className="font-display text-lg sm:text-xl font-bold text-[#00205b] mb-2.5">
                  2. Track Depletion & Carrydown
                </h5>
                <p className="text-slate-600 text-xs sm:text-sm leading-relaxed mb-6">
                  Bowling balls drag oil into the dry backend (carrydown), while the 10-board track begins hooking early.
                </p>
              </div>
              <div className="space-y-2 text-xs bg-slate-50 p-4 rounded-xl border border-slate-200">
                <div className="font-bold text-slate-800">Recommended Move:</div>
                <div className="text-slate-600">• Move feet 3-4 boards left</div>
                <div className="text-slate-600">• Switch to Hybrid / 3000 Grit</div>
              </div>
            </div>

            {/* Stage 3 */}
            <div className="card-usbc p-8 border-t-4 border-t-[#c8102e] flex flex-col justify-between h-full shadow-sm">
              <div>
                <div className="flex items-center justify-between mb-4">
                  <span className="px-3 py-1 rounded-full bg-red-50 text-red-800 text-[10px] font-bold uppercase">
                    Games 4+: The Burn
                  </span>
                  <span className="text-xs text-slate-400 font-bold">100+ Shots</span>
                </div>
                <h5 className="font-display text-lg sm:text-xl font-bold text-[#00205b] mb-2.5">
                  3. Front Friction & Deep Inside
                </h5>
                <p className="text-slate-600 text-xs sm:text-sm leading-relaxed mb-6">
                  The first 15 feet lose oil entirely. Balls hook at the arrows unless lofted or thrown deep inside.
                </p>
              </div>
              <div className="space-y-2 text-xs bg-slate-50 p-4 rounded-xl border border-slate-200">
                <div className="font-bold text-slate-800">Recommended Move:</div>
                <div className="text-slate-600">• Stand 25+ board, launch over 15</div>
                <div className="text-slate-600">• Clean Pearl Reactive / Polished</div>
              </div>
            </div>

          </div>

        </div>
      )}

      {/* TAB 3: 1-HAND VS 2-HAND TELEMETRY BREAKDOWN */}
      {activeTab === 'mechanics' && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-stretch">
          
          {/* 1-Handed Analysis */}
          <div className="card-usbc p-8 sm:p-10 border-t-4 border-t-[#00205b] flex flex-col justify-between h-full shadow-sm">
            <div>
              <div className="flex items-center justify-between border-b border-slate-100 pb-4 mb-6">
                <div className="font-display font-bold text-xl text-[#00205b]">Traditional 1-Handed Delivery</div>
                <span className="text-xs font-bold px-3 py-1 rounded-full bg-blue-50 text-[#00205b] border border-blue-200">
                  PENDULUM TIMING
                </span>
              </div>

              <div className="space-y-4 text-sm">
                <div className="flex justify-between py-2 border-b border-slate-100">
                  <span className="text-slate-500 font-medium">Pushaway Timing:</span>
                  <span className="text-[#00205b] font-bold">Synced with Step 2 (5-step approach)</span>
                </div>
                <div className="flex justify-between py-2 border-b border-slate-100">
                  <span className="text-slate-500 font-medium">Torso Spine Tilt:</span>
                  <span className="text-[#00205b] font-bold">30° - 38° at foul line</span>
                </div>
                <div className="flex justify-between py-2 border-b border-slate-100">
                  <span className="text-slate-500 font-medium">Typical Rev Rate:</span>
                  <span className="text-[#00205b] font-bold">300 - 400 RPM</span>
                </div>
                <div className="flex justify-between py-2">
                  <span className="text-slate-500 font-medium">Key Mechanical Focus:</span>
                  <span className="text-[#c8102e] font-bold">Relaxed swing slot & flat spot slide</span>
                </div>
              </div>
            </div>
          </div>

          {/* 2-Handed Analysis */}
          <div className="card-usbc p-8 sm:p-10 border-t-4 border-t-[#c8102e] flex flex-col justify-between h-full shadow-md">
            <div>
              <div className="flex items-center justify-between border-b border-slate-100 pb-4 mb-6">
                <div className="font-display font-bold text-xl text-[#00205b]">Modern 2-Handed Delivery</div>
                <span className="text-xs font-bold px-3 py-1 rounded-full bg-red-50 text-[#c8102e] border border-red-200">
                  ROTATIONAL TORQUE
                </span>
              </div>

              <div className="space-y-4 text-sm">
                <div className="flex justify-between py-2 border-b border-slate-100">
                  <span className="text-slate-500 font-medium">Hinge & Pushaway:</span>
                  <span className="text-[#c8102e] font-bold">Late hinge with hop-step acceleration</span>
                </div>
                <div className="flex justify-between py-2 border-b border-slate-100">
                  <span className="text-slate-500 font-medium">Torso Spine Tilt:</span>
                  <span className="text-[#c8102e] font-bold">45° - 55° with lateral spine flexion</span>
                </div>
                <div className="flex justify-between py-2 border-b border-slate-100">
                  <span className="text-slate-500 font-medium">Typical Rev Rate:</span>
                  <span className="text-[#c8102e] font-bold">450 - 550+ RPM</span>
                </div>
                <div className="flex justify-between py-2">
                  <span className="text-slate-500 font-medium">Key Mechanical Focus:</span>
                  <span className="text-[#00205b] font-bold">Hip-shoulder separation & speed balance</span>
                </div>
              </div>
            </div>
          </div>

        </div>
      )}

    </div>
  );
};
