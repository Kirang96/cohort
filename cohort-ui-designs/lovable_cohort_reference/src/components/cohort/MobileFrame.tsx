import { cn } from "@/lib/utils";

interface MobileFrameProps {
  children: React.ReactNode;
  className?: string;
}

export const MobileFrame = ({ children, className }: MobileFrameProps) => {
  return (
    <div className={cn("mobile-container relative", className)}>
      {children}
    </div>
  );
};
