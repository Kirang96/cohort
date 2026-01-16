import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Coins, Check, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

interface PurchaseCreditsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onPurchase: (amount: number) => void;
}

const creditPackages = [
  { credits: 50, price: 99, popular: false },
  { credits: 150, price: 249, popular: true },
  { credits: 500, price: 699, popular: false },
];

export const PurchaseCreditsModal = ({ 
  isOpen, 
  onClose, 
  onPurchase 
}: PurchaseCreditsModalProps) => {
  const [selectedPackage, setSelectedPackage] = useState<number | null>(1);
  const [isProcessing, setIsProcessing] = useState(false);

  const handlePurchase = () => {
    if (selectedPackage === null) return;
    
    setIsProcessing(true);
    
    // Simulate Razorpay payment flow
    setTimeout(() => {
      setIsProcessing(false);
      onPurchase(creditPackages[selectedPackage].credits);
      onClose();
    }, 2000);
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-sm mx-auto rounded-2xl p-0 overflow-hidden border-border/50">
        <div className="p-6 pb-4">
          <DialogHeader>
            <DialogTitle className="text-xl font-semibold text-center">
              Get More Credits
            </DialogTitle>
          </DialogHeader>

          <p className="text-sm text-muted-foreground text-center mt-2 mb-6">
            Credits are used to join pools and unlock features
          </p>

          {/* Package selection */}
          <div className="space-y-3">
            {creditPackages.map((pkg, index) => (
              <button
                key={index}
                onClick={() => setSelectedPackage(index)}
                className={cn(
                  "w-full flex items-center justify-between p-4 rounded-xl border-2 transition-all duration-200",
                  selectedPackage === index
                    ? "border-primary bg-primary/5"
                    : "border-border hover:border-border/80 bg-card"
                )}
              >
                <div className="flex items-center gap-3">
                  <div className={cn(
                    "w-10 h-10 rounded-full flex items-center justify-center",
                    selectedPackage === index 
                      ? "bg-primary text-primary-foreground" 
                      : "bg-secondary text-secondary-foreground"
                  )}>
                    <Coins className="w-5 h-5" />
                  </div>
                  <div className="text-left">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-foreground">{pkg.credits} Credits</span>
                      {pkg.popular && (
                        <span className="text-2xs font-semibold uppercase tracking-wider px-2 py-0.5 rounded-full bg-primary/15 text-primary">
                          Popular
                        </span>
                      )}
                    </div>
                    <span className="text-xs text-muted-foreground">
                      ₹{(pkg.price / pkg.credits).toFixed(1)}/credit
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-lg font-semibold text-foreground">₹{pkg.price}</span>
                  {selectedPackage === index && (
                    <div className="w-5 h-5 rounded-full bg-primary flex items-center justify-center">
                      <Check className="w-3 h-3 text-primary-foreground" />
                    </div>
                  )}
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* Payment footer */}
        <div className="p-6 pt-4 bg-secondary/30 border-t border-border/50">
          <Button
            variant="cohort"
            size="cohort"
            onClick={handlePurchase}
            disabled={selectedPackage === null || isProcessing}
            className="w-full"
          >
            {isProcessing ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Processing...</span>
              </>
            ) : (
              <>
                <span>Pay with Razorpay</span>
              </>
            )}
          </Button>
          <p className="text-2xs text-muted-foreground text-center mt-3">
            Secure payment powered by Razorpay
          </p>
        </div>
      </DialogContent>
    </Dialog>
  );
};
