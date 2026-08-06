import Link from '@docusaurus/Link';
import CodeBlock from '@theme/CodeBlock';
import Heading from '@theme/Heading';
import Layout from '@theme/Layout';
import type { ReactNode } from 'react';
import styles from './index.module.css';

const EXAMPLE = `const { currentFrame } = useVideoCompositionPlayer({
  composition,
  width, height,
  autoPlay: true,
  drawFrame: ({ canvas, currentTime, frames }) => {
    'worklet';
    // draw anything with the Skia canvas API —
    // video frames arrive as GPU textures
  },
});

return (
  <Canvas style={{ width, height }}>
    <Image image={currentFrame} width={width} height={height} />
  </Canvas>
);`;

const FEATURES: { title: string; body: string }[] = [
  {
    title: '🎞 GPU-fast decoding',
    body: 'Video frames are decoded straight into GPU textures and wrapped as SkImages — no CPU round trip, no per-frame copies.',
  },
  {
    title: '🖌 Draw with Skia',
    body: 'Compose frames with the full React Native Skia imperative API inside a worklet: shaders, paragraphs, filters, anything.',
  },
  {
    title: '🪞 Preview = Export',
    body: 'The same drawFrame worklet renders the live preview and the exported file. What you see is exactly what you save.',
  },
  {
    title: '🎵 Audio mixing',
    body: 'Play video soundtracks and standalone audio items in sync during playback, mixed into an AAC track on export.',
  },
  {
    title: '⚡️ Hardware encoders',
    body: 'Export through MediaCodec on Android and AVAssetWriter on iOS, feeding the encoder GPU textures with zero readback.',
  },
  {
    title: '📱 Device-aware',
    body: 'Query Android decoding/encoding capabilities to pick a resolution, frame rate and bit rate the device actually supports.',
  },
];

export default function Home(): ReactNode {
  return (
    <Layout description="Video playback, compositing and encoding for React Native Skia">
      <header className={styles.hero}>
        <Heading as="h1" className={styles.heroTitle}>
          Video, meet <span>Skia</span>
        </Heading>
        <p className={styles.heroSubtitle}>
          Decode videos into GPU textures, compose them frame by frame with the
          React Native Skia canvas, and export the result through hardware
          encoders — audio included.
        </p>
        <div className={styles.buttons}>
          <Link className="button button--primary button--lg" to="/docs/getting-started">
            Get Started
          </Link>
          <Link
            className={`button button--outline button--lg ${styles.heroGhostButton}`}
            href="https://github.com/AzzappApp/react-native-skia-video"
          >
            GitHub
          </Link>
        </div>
      </header>

      <main>
        <section className={styles.section}>
          <div className={styles.features}>
            {FEATURES.map((feature) => (
              <div key={feature.title} className={styles.feature}>
                <Heading as="h3">{feature.title}</Heading>
                <p>{feature.body}</p>
              </div>
            ))}
          </div>
        </section>

        <section className={styles.sectionAlt}>
          <div className={styles.codeExample}>
            <Heading as="h2" style={{ textAlign: 'center' }}>
              One worklet, every frame
            </Heading>
            <CodeBlock language="tsx">{EXAMPLE}</CodeBlock>
          </div>
        </section>

        <section className={styles.section}>
          <div className={styles.demo}>
            <img src="img/island-studio.gif" alt="Island Studio demo" />
            <div className={styles.demoText}>
              <Heading as="h2">Learn by example</Heading>
              <p>
                <strong>Island Studio</strong> is a complete tutorial app built
                on this library: montage of gallery clips, SkSL shader
                transitions, draggable text and image overlays, bundled
                royalty-free soundtracks and a hardware-encoded 1080p export —
                in about 15 small, documented files.
              </p>
              <div className={styles.buttons} style={{ justifyContent: 'flex-start' }}>
                <Link className="button button--primary" to="/docs/example">
                  Read the walkthrough
                </Link>
                <Link
                  className="button button--secondary button--outline"
                  href="https://github.com/mlecoq/react-native-video-editor"
                >
                  Browse the code
                </Link>
              </div>
            </div>
          </div>
        </section>
      </main>
    </Layout>
  );
}
