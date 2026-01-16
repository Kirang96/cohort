import { useState } from "react";
import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { PoolDotsGrid } from "./PoolDotsGrid";
import { CreditsDisplay } from "./CreditsDisplay";
import { PurchaseCreditsModal } from "./PurchaseCreditsModal";
import { PoolHistory } from "./PoolHistory";
import { Sparkles, ChevronRight } from "lucide-react";

type CircleStatus = "forming" | "connecting" | "revealed";

interface HomeScreenProps {
  userName: string;
  onJoinPool: () => void;
  onViewDetails: () => void;
}

export const HomeScreen = ({ userName, onJoinPool, onViewDetails }: HomeScreenProps) => {
  const [credits, setCredits] = useState(125);
  const [isPurchaseModalOpen, setIsPurchaseModalOpen] = useState(false);

  // Mock circle data
  const circleStatus: CircleStatus = "forming";
  const maleCount = 12;
  const femaleCount = 11;
  const maxCount = 25;
  const daysRemaining = 2;
  const hasJoined = false;
  const isFull = false;

  const statusConfig = {
    forming: {
      label: "Forming",
      className: "status-badge-open",
      message: `Circle closes in ${daysRemaining} day${daysRemaining > 1 ? "s" : ""}`,
    },
    connecting: {
      label: "Connecting",
      className: "status-badge-progress",
      message: "Finding meaningful connections",
    },
    revealed: {
      label: "Revealed",
      className: "status-badge-ready",
      message: "Your connections await",
    },
  };

  const currentStatus = statusConfig[circleStatus];

  const handlePurchaseCredits = (amount: number) => {
    setCredits(prev => prev + amount);
  };

  return (
    <div className="screen-wrapper pb-24">
      {/* Header with greeting and credits */}
      <motion.div 
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex items-start justify-between mb-10"
      >
        <div>
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider mb-1">
            Welcome back
          </p>
          <h1 className="text-3xl font-display text-foreground tracking-tight">
            {userName}
          </h1>
        </div>
        <CreditsDisplay 
          credits={credits} 
          onPurchase={() => setIsPurchaseModalOpen(true)} 
        />
      </motion.div>

      {/* Circle Header - No card, just content */}
      <motion.div 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.1 }}
        className="mb-8"
      >
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-3">
            <motion.div 
              whileHover={{ rotate: 15 }}
              className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center"
            >
              <Sparkles className="w-4 h-4 text-primary" />
            </motion.div>
            <div>
              <h2 className="font-display text-xl text-foreground">January Circle</h2>
              <p className="text-xs text-muted-foreground">Kochi</p>
            </div>
          </div>
          <motion.span 
            initial={{ scale: 0.9 }}
            animate={{ scale: 1 }}
            className={cn("status-badge", currentStatus.className)}
          >
            {currentStatus.label}
          </motion.span>
        </div>
        <p className="text-sm text-muted-foreground mt-3 pl-11">
          {currentStatus.message}
        </p>
      </motion.div>

      {/* Dots Grid Visualization - Minimal container */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
        className="mb-8"
      >
        <PoolDotsGrid 
          maleCount={maleCount}
          femaleCount={femaleCount}
          maxCount={maxCount}
        />
      </motion.div>

      {/* Stats Row - Simple inline, no card */}
      <motion.div 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.25 }}
        className="flex items-center justify-between text-sm mb-8 py-4 border-y border-border/40"
      >
        <motion.div 
          whileHover={{ scale: 1.05 }}
          className="text-center cursor-default flex-1"
        >
          <p className="text-lg font-semibold text-foreground">{maleCount + femaleCount}</p>
          <p className="text-2xs text-muted-foreground uppercase tracking-wide">In Circle</p>
        </motion.div>
        <div className="h-8 w-px bg-border/50" />
        <motion.div 
          whileHover={{ scale: 1.05 }}
          className="text-center cursor-default flex-1"
        >
          <p className="text-lg font-semibold text-foreground">{maxCount * 2 - maleCount - femaleCount}</p>
          <p className="text-2xs text-muted-foreground uppercase tracking-wide">Spots Open</p>
        </motion.div>
        <div className="h-8 w-px bg-border/50" />
        <motion.div 
          whileHover={{ scale: 1.05 }}
          className="text-center cursor-default flex-1"
        >
          <p className="text-lg font-semibold text-foreground">{daysRemaining}d</p>
          <p className="text-2xs text-muted-foreground uppercase tracking-wide">Until Close</p>
        </motion.div>
      </motion.div>

      {/* Action Button - Standalone, no card wrapper */}
      <motion.div 
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
        className="mb-10"
      >
        {!hasJoined && circleStatus === "forming" && (
          <motion.div whileTap={{ scale: 0.98 }}>
            <Button
              variant="cohort"
              size="cohort"
              onClick={onJoinPool}
              disabled={isFull}
              className="group"
            >
              {isFull ? "Circle is balanced" : (
                <>
                  <span>Enter the Circle</span>
                  <span className="text-primary-foreground/70 text-sm ml-2">
                    (10 credits)
                  </span>
                </>
              )}
            </Button>
          </motion.div>
        )}

        {hasJoined && (
          <motion.div whileTap={{ scale: 0.98 }}>
            <Button
              variant="cohort-secondary"
              size="cohort"
              onClick={onViewDetails}
              className="group"
            >
              <span>View Your Circle</span>
              <ChevronRight className="w-4 h-4 ml-1 group-hover:translate-x-0.5 transition-transform" />
            </Button>
          </motion.div>
        )}
      </motion.div>

      {/* Section Divider */}
      <div className="editorial-divider flex justify-center mb-8">
        <span>Your Journey</span>
      </div>

      {/* Circle History Section */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.35 }}
      >
        <PoolHistory />
      </motion.div>

      {/* Info Footer - Simpler */}
      <div className="mt-10 animate-fade-in stagger-3">
        <p className="text-xs text-muted-foreground text-center leading-relaxed max-w-xs mx-auto">
          Each Circle runs on its own timeline. When it closes, 
          you'll receive curated connections based on genuine compatibility.
        </p>
      </div>

      {/* Purchase Credits Modal */}
      <PurchaseCreditsModal
        isOpen={isPurchaseModalOpen}
        onClose={() => setIsPurchaseModalOpen(false)}
        onPurchase={handlePurchaseCredits}
      />
    </div>
  );
};
