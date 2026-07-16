import { Star } from "lucide-react";
import { brand, productImages } from "../data/mock";

export default function Hero() {
  return (
    <section className="relative overflow-hidden pt-28 md:pt-32 pb-16 md:pb-20">
      <div className="max-w-[1360px] mx-auto px-6 md:px-10 grid md:grid-cols-2 gap-10 md:gap-16 items-center">
        <div className="tilt-in">
          <div className="flex items-center gap-2 mb-6">
            <div className="flex">
              {[...Array(5)].map((_, i) => (
                <Star key={i} size={14} className="fill-[#1a1a1a] text-[#1a1a1a]" />
              ))}
            </div>
            <span className="text-[13px] text-[#3a3a3a]">30,000+ verified 5-star reviews</span>
          </div>

          <h1 className="hero-title text-[64px] leading-[0.95] md:text-[112px] md:leading-[0.92]">
            BLOCK in?<br />Lock in.
          </h1>

          <p className="mt-6 text-[16px] md:text-[17px] text-[#3a3a3a] max-w-[520px] leading-relaxed">
            {brand.name} is a small physical device that locks distracting apps with a tap. Built for India. Made to help you <em className="italic">lock in</em>.
          </p>

          <div className="mt-8 flex flex-wrap items-center gap-4">
            <a href="#buy" className="btn-ink">
              Shop Now — ₹{brand.price}
            </a>
            <a href="#how" className="btn-outline-ink">How it works</a>
          </div>

          <div className="mt-8 flex items-center gap-6 text-[12.5px] text-[#5a5a5a]">
            <span>✓ Free shipping across India</span>
            <span className="hidden sm:inline">✓ 14-day money-back</span>
            <span className="hidden md:inline">✓ No subscription</span>
          </div>
        </div>

        <div className="relative">
          <div className="aspect-[4/5] md:aspect-[5/6] w-full rounded-[28px] overflow-hidden relative grain">
            <img
              src={productImages.hero}
              alt="BLOCK on a wooden desk with a phone and coffee"
              className="w-full h-full object-cover"
              loading="eager"
            />
          </div>
          <div className="absolute -bottom-6 -left-6 hidden md:flex bg-[#f2ede4] border border-[#e0d8c7] rounded-2xl p-4 shadow-[0_18px_40px_-20px_rgba(0,0,0,0.25)] card-lift">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-[#1a1a1a] text-[#f2ede4] flex items-center justify-center text-[11px] font-medium">BLOCK</div>
              <div className="text-[12.5px] leading-tight">
                <div className="font-medium">Deep Focus — ON</div>
                <div className="text-[#6b6b6b]">Locked until 6:00 PM</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
