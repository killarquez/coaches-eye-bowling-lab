import React from 'react';
import { Calendar, FileCheck } from 'lucide-react';

interface MobileQuickBarProps {
  onBookClick: () => void;
  onOpenDiagnostic: () => void;
}

export const MobileQuickBar: React.FC<MobileQuickBarProps> = ({
  onBookClick,
  onOpenDiagnostic
}) => {
  return (
    <div className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-md border-t border-slate-200 p-3 px-4 shadow-2xl flex items-center justify-between gap-3">
      <button
        onClick={onOpenDiagnostic}
        className="flex-1 py-2.5 px-3 rounded-xl bg-slate-100 border border-slate-300 text-[#00205b] font-bold text-xs flex items-center justify-center gap-1.5 shadow-sm cursor-pointer"
      >
        <FileCheck className="w-4 h-4 text-[#c8102e]" />
        <span>Diagnostic</span>
      </button>

      <button
        onClick={onBookClick}
        className="flex-1 btn-usbc-primary py-2.5 px-4 rounded-xl text-white font-bold text-xs uppercase tracking-wider flex items-center justify-center gap-1.5 shadow-md cursor-pointer"
      >
        <Calendar className="w-4 h-4" />
        <span>Book ($65+)</span>
      </button>
    </div>
  );
};
