"""
Programmatic prompt builder for RCA reasoning refinement.

The prompt is constructed entirely from structured data — no hardcoded
strings or template literals that could introduce hallucinated content.
The LLM is instructed to ONLY rephrase the existing reasoning, never
to add new conclusions.
"""

from app.models.rca import RcaRequest, RcaFinding


def build_rca_reasoning_prompt(
    request: RcaRequest,
    finding: RcaFinding,
) -> str:
    """
    Builds a prompt that asks the LLM to refine (not invent) the reasoning
    for a single RCA finding. The prompt contains ALL the data the LLM
    needs — it must not reference anything outside this context.
    """
    evidence_block = "\n".join(
        f"  - {e.metric}: {e.value} {e.unit} ({e.status}) — {e.description}"
        for e in finding.evidence
    )

    recommendations_block = "\n".join(
        f"  - [{r.priority}] {r.title}: {r.description}"
        for r in finding.recommendations
    )

    prompt = f"""You are a performance engineering expert. Your task is to
rewrite the following root cause analysis finding into a clearer, more
concise reasoning paragraph.

RULES:
1. You MUST only use the evidence and data provided below.
2. You MUST NOT introduce any new metrics, conclusions, or recommendations
   that are not already in the finding.
3. You MUST NOT speculate or hypothesize beyond what the data shows.
4. Keep the reasoning to 2-4 sentences.
5. Include specific metric values in your reasoning.
6. End with the single most important action.

SERVICE: {request.service_name}
BOTTLENECK TYPE: {finding.bottleneck_type.value}
CONFIDENCE: {finding.confidence:.0%}

EVIDENCE:
{evidence_block}

EXISTING REASONING:
{finding.reasoning}

RECOMMENDATIONS:
{recommendations_block}

Rewrite the reasoning section only. Return ONLY the refined reasoning
text, no headers, no bullet points, no markdown formatting."""

    return prompt


def build_rca_summary_prompt(
    request: RcaRequest,
    findings: list[RcaFinding],
) -> str:
    """
    Builds a prompt for generating a high-level summary of all findings.
    """
    findings_block = "\n\n".join(
        f"Finding {i + 1}: {f.bottleneck_type.value} (confidence: {f.confidence:.0%})\n"
        f"Reasoning: {f.reasoning}"
        for i, f in enumerate(findings[:5])
    )

    prompt = f"""You are a performance engineering expert. Summarize the
following root cause analysis findings for {request.service_name} in a
single concise paragraph (3-4 sentences).

RULES:
1. Only reference the findings provided below.
2. Mention the primary bottleneck and its confidence level.
3. Include 1-2 specific metric values.
4. End with the most critical recommended action.

FINDINGS:
{findings_block}

Write the summary as a single paragraph. No headers, no bullets."""

    return prompt
