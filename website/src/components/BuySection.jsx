import { brand, productImages } from "../data/mock";
import { Check } from "lucide-react";

export default function BuySection() {
  return (
    <section id="buy" className="py-20 md:py-28">
      <div className="max-w-[1360px] mx-auto px-6 md:px-10 grid md:grid-cols-2 gap-12 items-center">
        <div className="rounded-[28px] overflow-hidden aspect-square relative grain">
          <img src={productImages.lifestyle1} alt="BLOCK product" className="w-full h-full object-cover" />
        </div>
        <div>
          <p className="chip mb-4">One-time purchase</p>
          <h2 className="section-title text-[44px] md:text-[64px] leading-[0.95]">
            Meet {brand.name}.<br />Your focus, on tap.
          </h2>
          <div className="mt-6 flex items-baseline gap-3">
            <span className="font-display text-[46px] leading-none">₹{brand.price}</span>
            <span className="text-[16px] text-[#6b6b6b] line-through">₹{brand.originalPrice}</span>
            <span className="chip">Save ₹{brand.originalPrice - brand.price}</span>
          </div>

          <ul className="mt-6 space-y-2 text-[14.5px] text-[#2b2b2b]">
            {[
              "1 × BLOCK NFC device (Ivory)",
              "Free BLOCK app for iOS & Android",
              "Unlimited custom focus modes",
              "Free shipping in 2–7 days across India",
              "14-day money-back guarantee",
            ].map((f) => (
              <li key={f} className="flex items-start gap-2">
                <span className="check-mark mt-0.5"><Check size={13} strokeWidth={2.6} /></span>
                <span>{f}</span>
              </li>
            ))}
          </ul>

          <div className="mt-8 flex flex-wrap gap-3">
            <a href="#buy" className="btn-ink">Buy Now — ₹{brand.price}</a>
            <a href="#faq" className="btn-outline-ink">Have questions?</a>
          </div>
          <p className="mt-4 text-[12.5px] text-[#6b6b6b]">COD available • UPI / Cards / Netbanking</p>
        </div>
      </div>
    </section>
  );
}
