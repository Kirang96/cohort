import { useState, useEffect, useMemo } from "react";
import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { MapPin, X, ChevronLeft } from "lucide-react";
import { cn } from "@/lib/utils";

interface ProfileSetupScreenProps {
  onComplete: () => void;
  onCancel?: () => void;
  onBack?: () => void;
  isEditing?: boolean;
  initialData?: {
    name: string;
    age: string;
    gender: "male" | "female" | null;
    bio: string;
    interests: string[];
  };
}

const INTEREST_OPTIONS = [
  "Reading", "Music", "Travel", "Cooking", "Photography",
  "Art", "Movies", "Fitness", "Technology", "Nature",
  "Writing", "Gaming", "Coffee", "Hiking", "Dancing"
];

export const ProfileSetupScreen = ({ 
  onComplete, 
  onCancel,
  onBack,
  isEditing = false,
  initialData
}: ProfileSetupScreenProps) => {
  const [name, setName] = useState(initialData?.name || "");
  const [age, setAge] = useState(initialData?.age || "");
  const [gender, setGender] = useState<"male" | "female" | null>(initialData?.gender || null);
  const [bio, setBio] = useState(initialData?.bio || "");
  const [interests, setInterests] = useState<string[]>(initialData?.interests || []);

  // Track if any changes have been made
  const hasChanges = useMemo(() => {
    if (!isEditing) return true; // For new setup, always allow save when valid
    
    const initial = initialData || { name: "", age: "", gender: null, bio: "", interests: [] };
    return (
      name !== initial.name ||
      age !== initial.age ||
      gender !== initial.gender ||
      bio !== initial.bio ||
      JSON.stringify(interests.sort()) !== JSON.stringify([...initial.interests].sort())
    );
  }, [name, age, gender, bio, interests, initialData, isEditing]);

  const toggleInterest = (interest: string) => {
    if (interests.includes(interest)) {
      setInterests(interests.filter((i) => i !== interest));
    } else if (interests.length < 5) {
      setInterests([...interests, interest]);
    }
  };

  const isValid = name.length >= 2 && age && gender && interests.length >= 2;
  const canSave = isValid && hasChanges;

  return (
    <div className="screen-wrapper pb-28">
      {/* Header */}
      <motion.div 
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex items-center gap-4 mb-8"
      >
        {(isEditing && onCancel) || (!isEditing && onBack) ? (
          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={isEditing ? onCancel : onBack}
            className="w-10 h-10 rounded-full bg-secondary/50 flex items-center justify-center hover:bg-secondary transition-colors"
          >
            <ChevronLeft className="w-5 h-5 text-foreground" />
          </motion.button>
        ) : null}
        <div>
          <h1 className="text-2xl font-display text-foreground">
            {isEditing ? "Edit Profile" : "Set up your profile"}
          </h1>
          <p className="text-muted-foreground text-sm mt-1">
            {isEditing ? "Update your information" : "This helps us create balanced connections."}
          </p>
        </div>
      </motion.div>

      {/* Form Sections - Reduced card usage */}
      <div className="space-y-8 flex-1">
        {/* Basic Info Section */}
        <motion.div 
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-4">Basic Info</h3>
          
          <div className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm text-foreground">Name</label>
              <Input
                placeholder="Your first name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="h-12 rounded-xl border-border/50 bg-secondary/30 focus:bg-background"
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm text-foreground">Age</label>
              <Input
                type="number"
                placeholder="Your age"
                value={age}
                onChange={(e) => setAge(e.target.value)}
                className="h-12 rounded-xl border-border/50 bg-secondary/30 focus:bg-background"
                min={18}
                max={99}
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm text-foreground">Gender</label>
              <div className="flex gap-3">
                {(["male", "female"] as const).map((g) => (
                  <motion.button
                    key={g}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => setGender(g)}
                    className={cn(
                      "flex-1 h-12 rounded-xl text-sm font-medium transition-all border",
                      gender === g
                        ? "bg-primary text-primary-foreground border-primary"
                        : "bg-secondary/30 text-foreground border-border/50 hover:border-primary/50"
                    )}
                  >
                    {g.charAt(0).toUpperCase() + g.slice(1)}
                  </motion.button>
                ))}
              </div>
            </div>
          </div>
        </motion.div>

        {/* About You Section */}
        <motion.div 
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
        >
          <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-4">About You</h3>
          
          <div className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm text-foreground">
                Short bio <span className="text-muted-foreground text-xs">(optional)</span>
              </label>
              <Textarea
                placeholder="A few words about yourself..."
                value={bio}
                onChange={(e) => setBio(e.target.value.slice(0, 150))}
                className="resize-none rounded-xl border-border/50 bg-secondary/30 focus:bg-background min-h-[100px]"
                maxLength={150}
              />
              <p className="text-xs text-muted-foreground text-right">
                {bio.length}/150
              </p>
            </div>

            <div className="space-y-3">
              <label className="text-sm text-foreground">
                Interests <span className="text-muted-foreground text-xs">(pick 2-5)</span>
              </label>
              <div className="flex flex-wrap gap-2">
                {INTEREST_OPTIONS.map((interest, index) => {
                  const isSelected = interests.includes(interest);
                  return (
                    <motion.button
                      key={interest}
                      initial={{ opacity: 0, scale: 0.9 }}
                      animate={{ opacity: 1, scale: 1 }}
                      transition={{ delay: 0.2 + index * 0.02 }}
                      whileTap={{ scale: 0.95 }}
                      onClick={() => toggleInterest(interest)}
                      className={cn(
                        "interest-chip transition-all",
                        isSelected && "bg-primary text-primary-foreground border-primary"
                      )}
                    >
                      {interest}
                      {isSelected && <X className="w-3 h-3 ml-1" />}
                    </motion.button>
                  );
                })}
              </div>
            </div>
          </div>
        </motion.div>

        {/* Location Section */}
        <motion.div 
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-4">Location</h3>
          
          <div className="space-y-2">
            <label className="text-sm text-foreground">City</label>
            <div className="relative">
              <MapPin className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input
                value="Kochi"
                disabled
                className="h-12 pl-11 rounded-xl border-border/50 bg-muted/50 text-muted-foreground"
              />
            </div>
            <p className="text-xs text-muted-foreground">
              Currently available in Kochi
            </p>
          </div>
        </motion.div>
      </div>

      {/* Sticky Footer with Actions */}
      <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-md px-5 py-4 bg-background/95 backdrop-blur-sm border-t border-border/30">
        <div className="flex gap-3">
          {isEditing && onCancel && (
            <motion.div whileTap={{ scale: 0.98 }} className="flex-1">
              <Button
                variant="outline"
                size="lg"
                onClick={onCancel}
                className="w-full h-12 rounded-xl border-border/50"
              >
                Cancel
              </Button>
            </motion.div>
          )}
          <motion.div whileTap={{ scale: 0.98 }} className={isEditing && onCancel ? "flex-1" : "w-full"}>
            <Button
              variant="cohort"
              size="cohort"
              onClick={onComplete}
              disabled={!canSave}
              className="w-full"
            >
              {isEditing ? "Save Changes" : "Save & Continue"}
            </Button>
          </motion.div>
        </div>
      </div>
    </div>
  );
};
