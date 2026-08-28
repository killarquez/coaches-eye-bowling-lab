export interface CoachingGroupTier {
  bowlers: string;
  ratePerPerson: number;
  duration: string;
  description?: string;
}

export interface CoachingPackage {
  id: string;
  title: string;
  subtitle: string;
  price: number;
  priceNote?: string;
  formatType?: string;
  duration: string;
  badge?: string;
  popular?: boolean;
  idealFor: string;
  description: string;
  features: string[];
  groupTiers?: CoachingGroupTier[];
  color: 'red' | 'teal' | 'purple' | 'navy';
}


export interface DiagnosticData {
  fullName: string;
  email: string;
  phone: string;
  currentAverage: string;
  highSeries: string;
  bowlingStyle: '1-Handed' | '2-Handed';
  dominantHand: 'Right' | 'Left';
  arsenalTypes: string[];
  physicalLimitations: string;
  primaryGoal: string;
  preferredDays: string[];
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  isMinor?: boolean;
  parentGuardianName?: string;
  liabilityConsent: boolean;
  videoConsent: boolean;
  marketingMediaConsent: 'granted' | 'private_only';
  cancellationPolicyConsent: boolean;
  signedTimestamp?: string;
}



export interface ProShopPartner {
  name: string;
  location: string;
  distance: string;
  specialty: string;
  description: string;
  services: string[];
  contact: string;
  address: string;
}

export interface Testimonial {
  quote: string;
  author: string;
  role: string;
  statGain: string;
  avatarUrl?: string;
  league: string;
}
