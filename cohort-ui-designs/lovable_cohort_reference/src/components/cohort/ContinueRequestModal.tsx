import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Clock, Heart, X } from "lucide-react";

interface ContinueRequestModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSendRequest: () => void;
  onAcceptRequest: () => void;
  matchName: string;
  mode: "send" | "received";
  credits?: number;
}

export const ContinueRequestModal = ({
  isOpen,
  onClose,
  onSendRequest,
  onAcceptRequest,
  matchName,
  mode,
  credits = 5,
}: ContinueRequestModalProps) => {
  if (!isOpen) return null;

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-50 flex items-end justify-center"
      onClick={onClose}
    >
      {/* Backdrop */}
      <div className="absolute inset-0 bg-foreground/20 backdrop-blur-sm" />

      {/* Modal */}
      <motion.div
        initial={{ y: "100%" }}
        animate={{ y: 0 }}
        exit={{ y: "100%" }}
        transition={{ type: "spring", damping: 25, stiffness: 300 }}
        onClick={(e) => e.stopPropagation()}
        className="relative w-full max-w-md bg-background rounded-t-3xl p-6 pb-8"
      >
        {/* Handle */}
        <div className="w-10 h-1 bg-muted rounded-full mx-auto mb-6" />

        {/* Close button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 w-8 h-8 rounded-full bg-secondary/50 flex items-center justify-center hover:bg-secondary transition-colors"
        >
          <X className="w-4 h-4 text-muted-foreground" />
        </button>

        {mode === "send" ? (
          <>
            {/* Icon */}
            <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-6">
              <Clock className="w-8 h-8 text-primary" />
            </div>

            {/* Content */}
            <h2 className="font-display text-xl text-foreground text-center mb-2">
              Request More Time?
            </h2>
            <p className="text-sm text-muted-foreground text-center mb-6 max-w-[260px] mx-auto">
              Your conversation with <span className="font-medium text-foreground">{matchName}</span> is expiring soon. 
              Request to continue if you'd like more time.
            </p>

            {/* Cost info */}
            <div className="flex items-center justify-center gap-2 py-3 px-4 bg-secondary/30 rounded-xl mb-6">
              <span className="text-sm text-muted-foreground">Cost:</span>
              <span className="text-sm font-semibold text-foreground">{credits} credits</span>
            </div>

            {/* Actions */}
            <div className="space-y-3">
              <motion.div whileTap={{ scale: 0.98 }}>
                <Button
                  variant="cohort"
                  size="cohort"
                  onClick={onSendRequest}
                  className="w-full"
                >
                  Send Continue Request
                </Button>
              </motion.div>
              <motion.div whileTap={{ scale: 0.98 }}>
                <Button
                  variant="ghost"
                  size="lg"
                  onClick={onClose}
                  className="w-full h-12 text-muted-foreground"
                >
                  Let it expire
                </Button>
              </motion.div>
            </div>
          </>
        ) : (
          <>
            {/* Icon */}
            <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-6">
              <Heart className="w-8 h-8 text-primary" />
            </div>

            {/* Content */}
            <h2 className="font-display text-xl text-foreground text-center mb-2">
              Continue Request Received
            </h2>
            <p className="text-sm text-muted-foreground text-center mb-6 max-w-[260px] mx-auto">
              <span className="font-medium text-foreground">{matchName}</span> wants to continue 
              the conversation. Would you like more time together?
            </p>

            {/* Actions */}
            <div className="space-y-3">
              <motion.div whileTap={{ scale: 0.98 }}>
                <Button
                  variant="cohort"
                  size="cohort"
                  onClick={onAcceptRequest}
                  className="w-full"
                >
                  Accept & Continue
                </Button>
              </motion.div>
              <motion.div whileTap={{ scale: 0.98 }}>
                <Button
                  variant="ghost"
                  size="lg"
                  onClick={onClose}
                  className="w-full h-12 text-muted-foreground"
                >
                  Decline politely
                </Button>
              </motion.div>
            </div>
          </>
        )}
      </motion.div>
    </motion.div>
  );
};
