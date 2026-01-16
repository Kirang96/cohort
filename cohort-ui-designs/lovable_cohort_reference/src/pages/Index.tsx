import { useState, useEffect } from "react";
import { AnimatePresence } from "framer-motion";
import { MobileFrame } from "@/components/cohort/MobileFrame";
import { BottomNav } from "@/components/cohort/BottomNav";
import { PageTransition } from "@/components/cohort/PageTransition";
import { SplashScreen } from "@/components/cohort/SplashScreen";
import { LoginScreen } from "@/components/cohort/LoginScreen";
import { ProfileSetupScreen } from "@/components/cohort/ProfileSetupScreen";
import { OnboardingTutorial } from "@/components/cohort/OnboardingTutorial";
import { HomeScreen } from "@/components/cohort/HomeScreen";
import { PoolDetailsScreen } from "@/components/cohort/PoolDetailsScreen";
import { MatchesScreen } from "@/components/cohort/MatchesScreen";
import { ChatListScreen } from "@/components/cohort/ChatListScreen";
import { ChatScreen } from "@/components/cohort/ChatScreen";
import { ProfileScreen } from "@/components/cohort/ProfileScreen";
import { SafetyModal } from "@/components/cohort/SafetyModal";

type Screen = 
  | "splash"
  | "login" 
  | "setup" 
  | "onboarding"
  | "home" 
  | "poolDetails" 
  | "matches" 
  | "chatList" 
  | "chat" 
  | "profile"
  | "editProfile";

type Tab = "home" | "matches" | "chats" | "profile";

// Mock user data for editing
const MOCK_USER_DATA = {
  name: "Arjun",
  age: "28",
  gender: "male" as const,
  bio: "Software engineer who loves exploring new places and trying different cuisines. Always up for a good book recommendation.",
  interests: ["Reading", "Travel", "Coffee", "Technology"],
};

const Index = () => {
  const [currentScreen, setCurrentScreen] = useState<Screen>("splash");
  const [activeTab, setActiveTab] = useState<Tab>("home");
  const [activeChatId, setActiveChatId] = useState<string | null>(null);
  const [isSafetyModalOpen, setIsSafetyModalOpen] = useState(false);

  const handleSplashComplete = () => {
    setCurrentScreen("login");
  };

  const handleLogin = (isNewUser: boolean) => {
    setCurrentScreen(isNewUser ? "setup" : "home");
  };

  const handleProfileSetupComplete = () => {
    setCurrentScreen("onboarding");
  };

  const handleOnboardingComplete = () => {
    setCurrentScreen("home");
  };

  const handleEditProfileComplete = () => {
    setCurrentScreen("profile");
    setActiveTab("profile");
  };

  const handleBackToLogin = () => {
    setCurrentScreen("login");
  };

  const handleEditProfileCancel = () => {
    setCurrentScreen("profile");
    setActiveTab("profile");
  };

  const handleTabChange = (tab: Tab) => {
    setActiveTab(tab);
    if (tab === "home") setCurrentScreen("home");
    if (tab === "matches") setCurrentScreen("matches");
    if (tab === "chats") setCurrentScreen("chatList");
    if (tab === "profile") setCurrentScreen("profile");
  };

  const handleOpenChat = (chatId: string) => {
    setActiveChatId(chatId);
    setCurrentScreen("chat");
  };

  const handleBackFromChat = () => {
    setActiveChatId(null);
    setCurrentScreen("chatList");
    setActiveTab("chats");
  };

  const handleViewPoolDetails = () => {
    setCurrentScreen("poolDetails");
  };

  const handleBackFromPoolDetails = () => {
    setCurrentScreen("home");
  };

  const handleLogout = () => {
    setCurrentScreen("login");
    setActiveTab("home");
  };

  const handleEditProfile = () => {
    setCurrentScreen("editProfile");
  };

  const showBottomNav = ["home", "matches", "chatList", "profile"].includes(currentScreen);

  const renderScreen = () => {
    switch (currentScreen) {
      case "splash":
        return null; // Splash is rendered outside MobileFrame
      case "login":
        return <LoginScreen onLogin={handleLogin} />;
      case "setup":
        return (
          <ProfileSetupScreen 
            onComplete={handleProfileSetupComplete}
            onBack={handleBackToLogin}
            isEditing={false}
          />
        );
      case "onboarding":
        return <OnboardingTutorial onComplete={handleOnboardingComplete} />;
      case "home":
        return (
          <HomeScreen
            userName="Arjun"
            onJoinPool={() => {}}
            onViewDetails={handleViewPoolDetails}
          />
        );
      case "poolDetails":
        return <PoolDetailsScreen onBack={handleBackFromPoolDetails} />;
      case "matches":
        return <MatchesScreen onOpenChat={handleOpenChat} />;
      case "chatList":
        return <ChatListScreen onOpenChat={handleOpenChat} />;
      case "chat":
        return (
          <ChatScreen
            chatId={activeChatId || ""}
            onBack={handleBackFromChat}
            onOpenSafetyModal={() => setIsSafetyModalOpen(true)}
          />
        );
      case "profile":
        return (
          <ProfileScreen
            onLogout={handleLogout}
            onEditProfile={handleEditProfile}
          />
        );
      case "editProfile":
        return (
          <ProfileSetupScreen 
            onComplete={handleEditProfileComplete}
            onCancel={handleEditProfileCancel}
            isEditing={true}
            initialData={MOCK_USER_DATA}
          />
        );
      default:
        return null;
    }
  };

  return (
    <>
      {/* Splash Screen - Full screen overlay */}
      <AnimatePresence>
        {currentScreen === "splash" && (
          <SplashScreen onComplete={handleSplashComplete} />
        )}
      </AnimatePresence>

      {/* Main App */}
      {currentScreen !== "splash" && (
        <MobileFrame>
          <PageTransition screenKey={currentScreen}>
            {renderScreen()}
          </PageTransition>
          {showBottomNav && (
            <BottomNav activeTab={activeTab} onTabChange={handleTabChange} />
          )}
          <SafetyModal
            isOpen={isSafetyModalOpen}
            onClose={() => setIsSafetyModalOpen(false)}
          />
        </MobileFrame>
      )}
    </>
  );
};

export default Index;
