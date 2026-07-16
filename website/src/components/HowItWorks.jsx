import { steps, productImages } from "../data/mock";

export default function HowItWorks() {
  return (
    <section id="how" className="py-20 md:py-28">
      <div className="max-w-[1360px] mx-auto px-6 md:px-10">
        <div className="grid md:grid-cols-12 gap-10">
          <div className="md:col-span-5 md:sticky md:top-24 self-start">
            <p className="chip mb-5">How it works</p>
            <h2 className="section-title text-[44px] md:text-[60px]">A tap is all<br />it takes.</h2>
            <p className="mt-5 text-[#3a3a3a] text-[15.5px] max-w-md">
              BLOCK uses NFC to switch your phone in and out of focus modes. No batteries, no subscription — just a physical anchor for your attention.
            </p>
            <div className="mt-6 rounded-[24px] overflow-hidden aspect-[4/3] relative grain">
              <img src={productImages.minimalDesk2} alt="BLOCK in use" className="w-full h-full object-cover" />
            </div>
          </div>

          <div className="md:col-span-7 flex flex-col gap-4">
            {steps.map((s) => (
              <div key={s.n} className="card-lift bg-[#ece5d8] border border-[#e0d8c7] rounded-[22px] p-7 md:p-9">
                <div className="flex items-baseline gap-5">
                  <span className="font-display text-[44px] md:text-[54px] text-[#1a1a1a] leading-none">{s.n}</span>
                  <div>
                    <h3 className="font-sans-tight text-[22px] md:text-[26px] font-semibold">{s.title}</h3>
                    <p className="mt-2 text-[#3a3a3a] text-[15px] leading-relaxed max-w-lg">{s.body}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
