import Navbar from "../components/Navbar";
import Footer from "../components/Footer";
import { productImages, brand } from "../data/mock";

export default function OurStory() {
  return (
    <div className="min-h-screen bg-[#f2ede4] text-[#1a1a1a]">
      <Navbar />
      <section className="pt-32 pb-16 md:pb-24">
        <div className="max-w-[900px] mx-auto px-6 md:px-10">
          <p className="chip mb-6">Our Story</p>
          <h1 className="hero-title text-[52px] md:text-[88px] leading-[0.95]">
            We built {brand.name} because<br />we needed it too.
          </h1>
          <div className="mt-10 rounded-[28px] overflow-hidden aspect-[16/9] grain">
            <img src={productImages.focus1} alt="Our story" className="w-full h-full object-cover" />
          </div>
          <div className="mt-10 space-y-6 text-[16.5px] text-[#2b2b2b] leading-[1.75] font-sans-tight">
            <p>
              A few years ago, we noticed the same pattern in every friend, colleague, and family member: the phone was in the hand, the mind was somewhere else, and the day was gone.
            </p>
            <p>
              Willpower apps didn't work — the off-switch was always a tap away. Basic phones were too extreme. What we needed was something small, physical, and honest. Something you could tap once to enter your best hours.
            </p>
            <p>
              So we made {brand.name}. Designed in India, built for the way we actually live — loud, connected, and full of pings. One tap to lock in. One tap to come back.
            </p>
          </div>

          <div className="mt-14 grid md:grid-cols-3 gap-4">
            <div className="rounded-[22px] overflow-hidden aspect-[4/5] grain"><img src={productImages.focus2} alt="" className="w-full h-full object-cover" /></div>
            <div className="rounded-[22px] overflow-hidden aspect-[4/5] grain"><img src={productImages.phone1} alt="" className="w-full h-full object-cover" /></div>
            <div className="rounded-[22px] overflow-hidden aspect-[4/5] grain"><img src={productImages.minimalDesk} alt="" className="w-full h-full object-cover" /></div>
          </div>

          <div className="mt-16 text-center">
            <a href="/#buy" className="btn-ink">Get {brand.name} — ₹{brand.price}</a>
          </div>
        </div>
      </section>
      <Footer />
    </div>
  );
}
