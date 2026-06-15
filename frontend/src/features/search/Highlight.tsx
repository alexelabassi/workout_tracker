interface HighlightProps {
  fragments?: string[];
  fallback: string;
}

/**
 * Renders OpenSearch highlight fragments (which wrap matched terms in <mark>…</mark>) when present,
 * otherwise the plain text. The markup is produced by our own backend over our own indexed data, so
 * injecting it is safe here.
 */
export function Highlight({ fragments, fallback }: HighlightProps) {
  if (fragments && fragments.length > 0) {
    return <span dangerouslySetInnerHTML={{ __html: fragments.join(" … ") }} />;
  }
  return <span>{fallback}</span>;
}
