import { Coins, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";

interface CreditsDisplayProps {
  credits: number;
  onPurchase: () => void;
}

export const CreditsDisplay = ({ credits, onPurchase }: CreditsDisplayProps) => {
  return (
    <div className="flex items-center gap-2">
      <div className="credits-badge">
        <Coins className="w-4 h-4" />
        <span>{credits}</span>
      </div>
      <Button
        variant="ghost"
        size="icon"
        onClick={onPurchase}
        className="h-8 w-8 rounded-full bg-primary/10 text-primary hover:bg-primary/20"
      >
        <Plus className="w-4 h-4" />
      </Button>
    </div>
  );
};
