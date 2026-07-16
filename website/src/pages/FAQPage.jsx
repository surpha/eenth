import Navbar from "../components/Navbar";
import Footer from "../components/Footer";
import FAQ from "../components/FAQ";

export default function FAQPage() {
  return (
    <div className="min-h-screen bg-[#f2ede4] text-[#1a1a1a]">
      <Navbar />
      <div className="pt-24" />
      <FAQ />
      <Footer />
    </div>
  );
}
