'use client';

import { useEffect, useRef } from 'react';
import { ArrowRight, Box, Download, Settings2 } from 'lucide-react';
import { gsap } from 'gsap';

export default function Home() {
  const titleRef = useRef<HTMLHeadingElement>(null);
  const heroRef = useRef<HTMLElement>(null);

  useEffect(() => {
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduced) return;

    const ctx = gsap.context(() => {
      gsap.fromTo(titleRef.current, { y: 30, opacity: 0 }, { y: 0, opacity: 1, duration: 1.1, ease: 'power3.out' });
      gsap.fromTo('.hero-copy', { y: 24, opacity: 0 }, { y: 0, opacity: 1, duration: 0.9, delay: 0.15, ease: 'power3.out' });
      gsap.fromTo('.hero-actions', { y: 18, opacity: 0 }, { y: 0, opacity: 1, duration: 0.8, delay: 0.3, ease: 'power3.out' });
      gsap.fromTo('.feature-card', { y: 20, opacity: 0 }, { y: 0, opacity: 1, duration: 0.7, delay: 0.5, stagger: 0.08, ease: 'power2.out' });
    }, heroRef);

    return () => ctx.revert();
  }, []);

  return (
    <main ref={heroRef} className="site-shell">
      <div className="ambient-grid" aria-hidden="true" />
      <div className="ambient-glow" aria-hidden="true" />

      <nav className="topbar">
        <div className="brand-mark">
          <span className="brand-dot" />
          <span>CrazeLauncher</span>
        </div>
        <div className="nav-links">
          <a href="#features">Features</a>
          <a href="#launcher">Launcher</a>
          <a href="#about">About</a>
        </div>
      </nav>

      <section id="launcher" className="hero">
        <div className="hero-copy">
          <div className="eyebrow"><span /> NEXT-GEN MINECRAFT LAUNCHER</div>
          <h1 ref={titleRef}>PLAY<br /><em>WITHOUT LIMITS.</em></h1>
          <p>
            A cinematic control center for CrazeLauncher — instances, mods, modpacks and launcher tools in one focused web experience.
          </p>
          <div className="hero-actions">
            <button className="primary-action">Open Launcher <ArrowRight size={17} /></button>
            <button className="secondary-action">Explore Mods <Box size={17} /></button>
          </div>
        </div>

        <div className="scene-card" aria-label="CrazeLauncher cinematic preview">
          <div className="scene-stars" />
          <div className="scene-horizon" />
          <div className="scene-tile tile-a" />
          <div className="scene-tile tile-b" />
          <div className="scene-tile tile-c" />
          <div className="scene-overlay">
            <span>CRAZE / 01</span>
            <span>READY</span>
          </div>
          <div className="scene-center">
            <div className="scene-logo">C</div>
            <span>CRA​ZELAUNCHER</span>
          </div>
        </div>
      </section>

      <section id="features" className="features">
        <article className="feature-card">
          <Download size={19} />
          <div><strong>One-click installs</strong><span>Mods and modpacks without the clutter.</span></div>
        </article>
        <article className="feature-card">
          <Box size={19} />
          <div><strong>Instance control</strong><span>Keep every Minecraft setup organized.</span></div>
        </article>
        <article className="feature-card">
          <Settings2 size={19} />
          <div><strong>Launcher tools</strong><span>Performance, settings and status in one place.</span></div>
        </article>
      </section>

      <footer id="about">CRAZELAUNCHER WEB · BUILT FOR THE CRAZE ECOSYSTEM</footer>
    </main>
  );
}
