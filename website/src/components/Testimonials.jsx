import { Star } from "lucide-react";
import { testimonials, stats } from "../data/mock";

export default function Testimonials() {
  return (
    <section className="py-20 md:py-28 bg-[#ece5d8]">
      <div className="max-w-[1360px] mx-auto px-6 md:px-10">
        <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-6 mb-10">
          <div>
            <p className="chip mb-4">Loved across India</p>
            <h2 className="section-title text-[44px] md:text-[60px]">The proof<br />is in the focus.</h2>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6 md:gap-10">
            {stats.map((s, i) => (
              <div key={i}>
                <div className="font-display text-[32px] md:text-[38px] leading-none">{s.value}</div>
                <div className="text-[12.5px] text-[#6b6b6b] mt-1">{s.label}</div>
              </div>
            ))}
          </div>
        </div>

        <div className="grid md:grid-cols-3 gap-5">
          {testimonials.map((t, i) => (
            <div key={i} className="card-lift rounded-[22px] p-7 bg-[#f2ede4] border border-[#e0d8c7]">
              <div className="flex gap-1 mb-3">
                {[...Array(t.rating)].map((_, j) => (
                  <Star key={j} size={13} className="fill-[#1a1a1a] text-[#1a1a1a]" />
                ))}
              </div>
              <p className="text-[15px] leading-relaxed text-[#1a1a1a]">"{t.text}"</p>
              <div className="mt-5 pt-4 border-t border-[#e0d8c7] flex items-center justify-between">
                <div className="text-[13.5px] font-medium">{t.name}</div>
                <div className="text-[12.5px] text-[#6b6b6b]">{t.city}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
