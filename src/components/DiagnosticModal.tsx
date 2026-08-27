import React, { useEffect } from 'react';
import { X } from 'lucide-react';
import { DiagnosticForm } from './DiagnosticForm';
import type { DiagnosticData } from '../types';

interface DiagnosticModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialPackageId?: string;
  onProceedToBooking: (data: DiagnosticData) => void;
}

export const DiagnosticModal: React.FC<DiagnosticModalProps> = ({
  isOpen,
  onClose,
  initialPackageId,
  onProceedToBooking
}) => {
  // Prevent body scrolling when modal is open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-950/80 backdrop-blur-sm flex items-start sm:items-center justify-center p-3 sm:p-6 animate-in fade-in duration-150">
      <div className="relative w-full max-w-3xl my-auto max-h-[95vh] flex flex-col bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden">
        
        {/* Floating Close Button */}
        <button
          onClick={onClose}
          aria-label="Close diagnostic modal"
          className="absolute top-5 right-5 z-30 w-10 h-10 rounded-full bg-white/20 hover:bg-[#c8102e] border border-white/40 text-white flex items-center justify-center shadow-lg hover:scale-105 transition-all cursor-pointer"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Scrollable Form Content */}
        <div className="overflow-y-auto">
          <DiagnosticForm
            initialPackageId={initialPackageId}
            onProceedToBooking={(data) => {
              onProceedToBooking(data);
              onClose();
            }}
            onClose={onClose}
          />
        </div>


      </div>
    </div>
  );
};
