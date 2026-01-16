import { cn } from "@/lib/utils";
import { motion } from "framer-motion";
import { Home, Heart, MessageCircle, User } from "lucide-react";

interface BottomNavProps {
  activeTab: "home" | "matches" | "chats" | "profile";
  onTabChange: (tab: "home" | "matches" | "chats" | "profile") => void;
}

// Haptic feedback simulation
const hapticFeedback = {
  tap: { scale: [1, 0.88, 1], transition: { duration: 0.12 } },
};

export const BottomNav = ({ activeTab, onTabChange }: BottomNavProps) => {
  const tabs = [
    { id: "home" as const, icon: Home, label: "Home" },
    { id: "matches" as const, icon: Heart, label: "Connections" },
    { id: "chats" as const, icon: MessageCircle, label: "Chats" },
    { id: "profile" as const, icon: User, label: "Profile" },
  ];

  return (
    <nav className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[390px] bg-background/95 backdrop-blur-md border-t border-border/30 px-2 py-2 safe-area-pb z-50">
      <div className="flex items-center justify-around">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          
          return (
            <motion.button
              key={tab.id}
              onClick={() => onTabChange(tab.id)}
              whileTap={hapticFeedback.tap}
              className={cn(
                "flex flex-col items-center gap-1 px-4 py-2 rounded-2xl transition-all duration-200 relative min-w-[64px]",
                isActive 
                  ? "text-primary" 
                  : "text-muted-foreground hover:text-foreground active:bg-secondary/50"
              )}
            >
              <motion.div 
                className="relative"
                animate={{ 
                  y: isActive ? -2 : 0,
                  scale: isActive ? 1.1 : 1,
                }}
                transition={{ type: "spring", stiffness: 400, damping: 17 }}
              >
                <Icon className="w-5 h-5" strokeWidth={isActive ? 2.5 : 2} />
                {isActive && (
                  <motion.div
                    layoutId="activeIndicator"
                    className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full bg-primary"
                    initial={false}
                    transition={{ type: "spring", stiffness: 500, damping: 30 }}
                  />
                )}
              </motion.div>
              <motion.span 
                className={cn(
                  "text-[10px] transition-all",
                  isActive ? "font-semibold" : "font-medium"
                )}
                animate={{ 
                  scale: isActive ? 1.02 : 1,
                  opacity: isActive ? 1 : 0.8,
                }}
                transition={{ type: "spring", stiffness: 400, damping: 17 }}
              >
                {tab.label}
              </motion.span>
            </motion.button>
          );
        })}
      </div>
    </nav>
  );
};
