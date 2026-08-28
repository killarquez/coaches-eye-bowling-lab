import type { CoachingPackage, ProShopPartner, Testimonial } from '../types';

export const COACH_INFO = {
  name: "Alfredo Quilarquez",
  title: "USBC Level 1 Certified Coach & PBA Regional Competitor",
  locationName: "Bowlero West Covina",
  address: "675 S Glendora Ave, West Covina, CA 91790",
  phone: "(909) 764-4824",
  email: "coaching@alfredobowling.com",

  instagram: "@alfredoquilarquez_bowling",
  calendlyUrl: "https://calendly.com", // standard embed or simulated direct calendar
  bio: `Great bowling is a precise science of repetition, physics, and biomechanics. As an active PBA member, USBC Level 1 Certified Coach (actively pursuing USBC Bronze), and competitive tournament bowler, I help athletes see what they cannot feel in their own physical game.

My mission goes beyond basic drills: I am committed to preserving bowling as a true sport and athletic discipline. Whether working with two-handers looking to harness high-rev leverage or traditional one-handers mastering their swing plane, I bridge high-level competitive insight with accessible, actionable instruction. Backed by elite on-lane achievements—including a 730 sanctioned high series, sanctioned 300 games, and PBA Regional competition—I catch the most minute mechanical variations that cost you pinfall.`,
  stats: [
    { value: "730", label: "Official Sanctioned Series", sub: "Career High Series", icon: "Trophy" },
    { value: "2x", label: "Sanctioned 300 Games", sub: "Dozens in Tournament Practice", icon: "Flame" },
    { value: "PBA", label: "Member & Regional Competitor", sub: "Active Western Region Athlete", icon: "ShieldCheck" },
    { value: "Level 1", label: "USBC Certified Coach", sub: "SafeSport & RVP Cleared", icon: "Award" }
  ]
};


export const COACHING_PACKAGES: CoachingPackage[] = [
  {
    id: "tune-up",
    title: "60-Minute Quick Tune-Up",
    subtitle: "High-Speed Mechanical Diagnostic",
    price: 65,
    duration: "60 Minutes",
    formatType: "1-on-1 Private",
    idealFor: "Bowlers with an upcoming tournament or league match needing immediate timing, leverage, or release fixes.",
    description: "A laser-focused 1-on-1 session to diagnose and correct acute mechanical hitches, late timing, foul line stability, and ball release speed.",
    features: [
      "Frame-by-frame 240fps slow-motion video analysis",
      "Release vector & axis rotation check",
      "Foul line leverage & slide foot balance audit",
      "Immediate physical timing adjustment drills",
      "Post-session digital summary & drill prescription"
    ],
    color: "teal"
  },
  {
    id: "development-package",
    title: "5-Session Development Package",
    subtitle: "Comprehensive Overhaul & Progression",
    price: 295,
    duration: "5 x 60 Min Sessions",
    formatType: "1-on-1 Comprehensive",
    badge: "Most Popular • Best Value",
    popular: true,
    idealFor: "Dedicated league & tournament bowlers committed to restructuring their physical game for massive long-term consistency.",
    description: "Our premier comprehensive program that systematically rebuilds your approach, pushaway, swing slot, leverage, and spare conversion system.",
    features: [
      "Full 5-stage biomechanical progression blueprint",
      "Comprehensive video library with split-screen comparisons",
      "Complete 3-6-9 and 2-4-6 spare system mastery",
      "Arsenal evaluation & layout matching recommendations",
      "Tournament pressure simulation & target line adjustments",
      "Direct Coach text/video check-in access between sessions"
    ],
    color: "red"
  },
  {
    id: "group-camps",
    title: "4-Week Group Clinic",
    subtitle: "Structured Multi-Week Cohort (2 to 6 Bowlers)",
    price: 180,
    priceNote: "$150 – $180 / person",
    formatType: "Group Clinic (2–6 Bowlers)",
    duration: "4 Weeks (90–120 Min/Wk)",
    badge: "Group Tier Savings ($150–$180/pp)",
    groupTiers: [
      { bowlers: "1–2 Bowlers", ratePerPerson: 180, duration: "90 Min / Session (4 Wks)", description: "Standard base cohort rate" },
      { bowlers: "3–4 Bowlers", ratePerPerson: 170, duration: "105 Min / Session (+15m Bonus)", description: "Save $10/person + 15 min extra time" },
      { bowlers: "5 Bowlers", ratePerPerson: 160, duration: "120 Min / Session (+30m Bonus)", description: "Save $20/person + 30 min extra time" },
      { bowlers: "6 Bowlers (Cap)", ratePerPerson: 150, duration: "120 Min / Session (+30m Bonus)", description: "Save $30/person + 30 min extra time" }
    ],
    idealFor: "Groups of 2 to 6 bowlers looking for structured cohort learning, spare mastery, transition logic, and competitive group drills.",
    description: "Intensive 4-week group clinic covering the 4 pillars: Modern Approach Footwork, Leverage & Hand Positions, The Complete Spare Matrix, and Oil Transition Logic. The bigger the group, the lower the per-person rate ($150–$180/pp) and the longer the session (up to 120 min)!",
    features: [
      "Tiered pricing: $180 (1-2), $170 (3-4), $160 (5), $150 (6 bowlers / pp)",
      "Extended time: 90 min (1-2), 105 min (3-4), 120 min (5-6 bowlers)",
      "Week 1: Footwork tempo, pushaway timing & hinge mechanics",
      "Week 2: Flat spot release, rev rate generation & axis tilt",
      "Week 3: 3-6-9 Spare conversion system & single pin automaticity",
      "Week 4: Reading oil burn, lane transition & surface matching",
      "Includes all lane fees at Bowlero West Covina"
    ],
    color: "purple"
  },
  {
    id: "on-lane-strategy",
    title: "On-Lane Strategy Sessions",
    subtitle: "Real-Time Tournament Block Shadowing",
    price: 85,
    duration: "75 Minutes (Live Game Play)",
    formatType: "1-on-1 Live Match Play",
    idealFor: "Competitive bowlers wanting live match-play guidance, lane reading, ball motion adjustments, and tactical moves.",
    description: "Step onto the lanes with Coach Alfredo shadowing your game blocks in real-time to analyze moves, shape selection, surface changes, and mental composure.",
    features: [
      "Live oil pattern reading & breakpoint management",
      "Ball change timing & surface adjustment protocols",
      "In-game spare adjustment under transition",
      "Pre-shot routine & mental resilience coaching",
      "Full match-play telemetry & strike percentage review"
    ],
    color: "navy"
  }
];


export const TESTIMONIALS: Testimonial[] = [
  {
    quote: "Working with Alfredo completely revamped my timing at the foul line. My league average jumped 18 pins in one season, and my physical game finally feels effortless.",
    author: "Marcus T.",
    role: "USBC Sanctioned League Bowler",
    statGain: "+18 Pin Average Increase",
    league: "Bowlero West Covina Premier Scratch League"
  },
  {
    quote: "His eye for detail caught a subtle hitch in my swing that nobody else saw. The 3-6-9 spare system work made single-pin spares automatic under tournament pressure.",
    author: "Elena R.",
    role: "PBA Regional / Cal State Competitor",
    statGain: "94% Single-Pin Spare Conversion Rate",
    league: "SoCal Scratch Tournaments"
  },
  {
    quote: "As a 2-handed bowler, finding a coach who actually understands the biomechanics of the hip-shoulder separation and torso tilt is rare. Alfredo dialed in my rev-to-speed ratio in just 3 sessions.",
    author: "Devon K.",
    role: "2-Handed Tournament Bowler",
    statGain: "28 MPH & 480 RPM Balanced Game",
    league: "West Coast Masters Series"
  },
  {
    quote: "The video breakdown alone is worth 10x the price. Being able to see my slide foot decelerating and my shoulder dropping gave me immediate visual clarity.",
    author: "Carlos G.",
    role: "Competitive League Bowler",
    statGain: "First Career 750 Series Post-Coaching",
    league: "San Gabriel Valley Tri-City League"
  }
];

export const PRO_SHOP_PARTNERS: ProShopPartner[] = [
  {
    name: "Partner Pro Shop #1 (Pending Announcement)",
    location: "West Covina / SGV Area",
    distance: "Local Partner Network",
    specialty: "Precision Digital Layouts, Surface Resurfacing & 2LS Drilling",
    description: "Partnership agreements currently finalizing. We collaborate with master IBPSIA drillers to ensure dual-angle and 2LS layout methods complement Coach Alfredo's video biomechanics and PAP measurements.",
    services: ["Dual-Angle & 2LS Ball Drilling", "Digital Specto Axis Mapping", "Surface Grit & Polish Customization", "Custom Thumb & Finger Grip Molding"],
    contact: "Announcing Soon",
    address: "West Covina, CA"
  },
  {
    name: "Partner Pro Shop #2 (Pending Announcement)",
    location: "San Gabriel Valley, CA",
    distance: "Local Partner Network",
    specialty: "High-Performance Ball Fitting, PAP Verification & Tournament Prep",
    description: "Partnership agreements currently finalizing. Matching ball core dynamics (asymmetric vs symmetric) to your specific release specs (axis tilt, rotation, and ball speed) with direct on-lane spec sheets.",
    services: ["PAP (Positive Axis Point) Measurement", "Tournament Arsenal Blueprinting", "Weight Block & RG Optimization", "Interchangeable Thumb Insert Systems"],
    contact: "Announcing Soon",
    address: "West Covina Corridor, CA"
  }
];

export const PHILOSOPHY_PILLARS = [
  {
    title: "Biomechanical Precision & 240fps Video Analysis",
    description: "We don't guess—we measure. High-speed multi-angle video captures the micro-second release window, slide knee flexion, torso spine angle, and swing apex.",
    icon: "Video",
    highlight: "See what you cannot feel"
  },
  {
    title: "Adaptable to the Individual Athlete",
    description: "No dogmatic systems. Whether you are a high-rev 2-handed power player or a classic 1-handed stroker, we optimize around your natural flexibility and strength.",
    icon: "Target",
    highlight: "Customized to your body type"
  },
  {
    title: "Understanding the 'Why' for Pressure Execution",
    description: "When mechanics are backed by physics, you can self-diagnose in the 10th frame. You will understand how oil depletion, friction, and axis tilt dictate ball motion.",
    icon: "Compass",
    highlight: "Repeatable confidence under pressure"
  }
];

export const FAQS = [
  {
    q: "Where do coaching sessions take place?",
    a: "All private and group sessions are hosted at Bowlero West Covina (675 S Glendora Ave, West Covina, CA 91790). Lane fees during your lesson are factored into your session booking."
  },
  {
    q: "Do you coach both 1-Handed and 2-Handed bowlers?",
    a: "Absolutely. Coach Alfredo has deep expertise coaching traditional 1-handed (strokers, tweeners, crankers) as well as modern 2-handed athletes, focusing on hip hinge, spine angle preservation, and swing slot leverage."
  },
  {
    q: "What equipment should I bring to my first session?",
    a: "Bring your current bowling balls, bowling shoes, wrist supports (if used), and comfortable athletic clothing. We will test your current arsenal and measure your Positive Axis Point (PAP) during video capture."
  },
  {
    q: "How does the Preliminary Diagnostic Assessment work?",
    a: "Before your session, you'll complete our 2-minute diagnostic form outlining your current average, arsenal, bowling style, and specific pain points. Coach Alfredo reviews this data so not a single minute of on-lane time is wasted."
  },
  {
    q: "Can I book a multi-session package and schedule dates over time?",
    a: "Yes! When you purchase the 5-Session Development Package, you can schedule your first session immediately and space out remaining sessions across weeks to allow practice drill integration."
  },
  {
    q: "How do you coordinate with local Pro Shops?",
    a: "If Coach Alfredo identifies that your ball layout, thumb pitch, or span is inhibiting clean release or leverage, we provide a written layout spec sheet directly to your preferred driller or our trusted partner network."
  }
];

