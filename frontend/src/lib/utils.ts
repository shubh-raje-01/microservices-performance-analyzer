import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'
import type {
  SimulationStatus,
  MetricSeverity,
  RecommendationPriority,
  AnomalyType,
} from '../types'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatLatency(ms: number | null | undefined): string {
  if (ms == null) return '—'
  if (ms < 1000) return `${Math.round(ms)}ms`
  return `${(ms / 1000).toFixed(2)}s`
}

export function formatErrorRate(rate: number | null | undefined): string {
  if (rate == null) return '—'
  return `${(rate * 100).toFixed(2)}%`
}

export function formatThroughput(rps: number | null | undefined): string {
  if (rps == null) return '—'
  return `${rps.toFixed(1)} req/s`
}

export function formatScore(score: number | null | undefined): string {
  if (score == null) return '—'
  return `${score.toFixed(1)} / 100`
}

export function formatCount(n: number | null | undefined): string {
  if (n == null) return '—'
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`
  return n.toString()
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  return d.toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}

export function formatDuration(from: string | null, to: string | null): string {
  if (!from || !to) return '—'
  const diff = new Date(to).getTime() - new Date(from).getTime()
  if (diff < 0) return '—'
  const totalSeconds = Math.floor(diff / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  if (minutes === 0) return `${seconds}s`
  return `${minutes}m ${seconds}s`
}

export function timeAgo(iso: string | null | undefined): string {
  if (!iso) return '—'
  const diff = Date.now() - new Date(iso).getTime()
  const seconds = Math.floor(diff / 1000)
  if (seconds < 60) return `${seconds}s ago`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  return `${days}d ago`
}

export const statusColour: Record<SimulationStatus, string> = {
  PENDING: 'bg-gray-100 text-gray-700',
  RUNNING: 'bg-blue-100 text-blue-700',
  COMPLETED: 'bg-green-100 text-green-700',
  FAILED: 'bg-red-100 text-red-700',
  CANCELLED: 'bg-amber-100 text-amber-700',
}

export const severityColour: Record<MetricSeverity, string> = {
  NORMAL: 'bg-green-100 text-green-700',
  WARNING: 'bg-amber-100 text-amber-700',
  CRITICAL: 'bg-red-100 text-red-700',
}

export const priorityColour: Record<RecommendationPriority, string> = {
  HIGH: 'bg-red-100 text-red-700',
  MEDIUM: 'bg-amber-100 text-amber-700',
  LOW: 'bg-blue-100 text-blue-700',
}

export const anomalyColour: Record<AnomalyType, string> = {
  NONE: 'bg-green-100 text-green-700',
  LATENCY_SPIKE: 'bg-amber-100 text-amber-700',
  ERROR_BURST: 'bg-red-100 text-red-700',
  THROUGHPUT_DROP: 'bg-orange-100 text-orange-700',
  SATURATION: 'bg-purple-100 text-purple-700',
  MEMORY_PRESSURE: 'bg-pink-100 text-pink-700',
  CASCADING_FAILURE: 'bg-red-100 text-red-700',
  UNKNOWN: 'bg-gray-100 text-gray-700',
}

export const healthColour: Record<string, string> = {
  HEALTHY: 'bg-green-100 text-green-700',
  DEGRADED: 'bg-amber-100 text-amber-700',
  CRITICAL: 'bg-red-100 text-red-700',
}
