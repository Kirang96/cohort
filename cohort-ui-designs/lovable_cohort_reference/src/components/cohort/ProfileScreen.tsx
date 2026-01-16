import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { MapPin, LogOut, Pencil, Sparkles } from "lucide-react";

interface ProfileScreenProps {
  onLogout: () => void;
  onEditProfile: () => void;
}

export const ProfileScreen = ({ onLogout, onEditProfile }: ProfileScreenProps) => {
  // Mock user data
  const user = {
    name: "Arjun",
    age: 28,
    gender: "Male",
    bio: "Software engineer who loves exploring new places and trying different cuisines. Always up for a good book recommendation.",
    interests: ["Reading", "Travel", "Coffee", "Technology"],
    city: "Kochi",
    circlesJoined: 5,
    connectionsReceived: 12,
  };

  return (
    <div className="screen-wrapper pb-24">
      {/* Header */}
      <motion.div 
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex items-center justify-between mb-8"
      >
        <h1 className="text-2xl font-display text-foreground">Profile</h1>
        <motion.button
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          onClick={onEditProfile}
          className="w-10 h-10 rounded-full bg-secondary/50 flex items-center justify-center hover:bg-secondary transition-colors"
        >
          <Pencil className="w-4 h-4 text-foreground" />
        </motion.button>
      </motion.div>

      {/* Profile Header - No card */}
      <motion.div 
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="flex items-center gap-4 mb-8"
      >
        <div className="w-20 h-20 rounded-full bg-gradient-to-br from-primary/20 to-primary/5 flex items-center justify-center">
          <span className="text-3xl font-display text-primary">
            {user.name[0]}
          </span>
        </div>
        <div>
          <h2 className="text-2xl font-display text-foreground">{user.name}</h2>
          <p className="text-sm text-muted-foreground">
            {user.age} years · {user.gender}
          </p>
          <div className="flex items-center gap-1 mt-1 text-xs text-muted-foreground">
            <MapPin className="w-3 h-3" />
            <span>{user.city}</span>
          </div>
        </div>
      </motion.div>

      {/* Stats Row - Simple inline */}
      <motion.div 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.15 }}
        className="flex items-center justify-around py-5 border-y border-border/40 mb-8"
      >
        <div className="text-center">
          <p className="text-xl font-semibold text-foreground">{user.circlesJoined}</p>
          <p className="text-xs text-muted-foreground uppercase tracking-wide mt-1">Circles</p>
        </div>
        <div className="h-8 w-px bg-border/50" />
        <div className="text-center">
          <p className="text-xl font-semibold text-foreground">{user.connectionsReceived}</p>
          <p className="text-xs text-muted-foreground uppercase tracking-wide mt-1">Connections</p>
        </div>
      </motion.div>

      {/* Bio Section */}
      {user.bio && (
        <motion.div 
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="mb-8"
        >
          <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">About</h3>
          <p className="text-sm text-foreground leading-relaxed">
            {user.bio}
          </p>
        </motion.div>
      )}

      {/* Interests Section */}
      <motion.div 
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.25 }}
        className="mb-10"
      >
        <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">Interests</h3>
        <div className="flex flex-wrap gap-2">
          {user.interests.map((interest, index) => (
            <motion.span 
              key={interest} 
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.3 + index * 0.05 }}
              className="interest-chip"
            >
              {interest}
            </motion.span>
          ))}
        </div>
      </motion.div>

      {/* Divider */}
      <div className="editorial-divider flex justify-center mb-8">
        <span>Settings</span>
      </div>

      {/* Logout - Simple, no card */}
      <motion.div 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.35 }}
        whileTap={{ scale: 0.98 }}
      >
        <Button
          variant="ghost"
          size="lg"
          onClick={onLogout}
          className="w-full justify-start text-destructive hover:text-destructive hover:bg-destructive/5 h-12"
        >
          <LogOut className="w-4 h-4 mr-3" />
          Sign Out
        </Button>
      </motion.div>
    </div>
  );
};
