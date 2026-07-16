import { useState } from "react";
import Navbar from "../components/Navbar";
import Hero from "../components/Hero";
import PressBar from "../components/PressBar";
import Modes from "../components/Modes";
import Comparison from "../components/Comparison";
import HowItWorks from "../components/HowItWorks";
import Testimonials from "../components/Testimonials";
import BuySection from "../components/BuySection";
import FAQ from "../components/FAQ";
import Footer from "../components/Footer";

export default function Home() {
  const [cartOpen, setCartOpen] = useState(false);

  return (
    <div className="min-h-screen bg-[#f2ede4] text-[#1a1a1a]">
      <Navbar />
      <Hero />
      <PressBar />
      <Modes />
      <Comparison />
      <HowItWorks />
      <Testimonials />
      <BuySection />
      <FAQ limit={5} />
      <Footer />
    </div>
  );
}
