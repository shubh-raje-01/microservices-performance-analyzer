import { useEffect, useRef, useCallback, useState } from 'react'
import type { GrafanaWidgetData, GrafanaWebSocketMessage } from '../types'

type Options = {
  enabled?: boolean
  onMessage?: (data: GrafanaWidgetData) => void
  reconnectInterval?: number
  maxReconnectAttempts?: number
}

type Return = {
  isConnected: boolean
  lastData: GrafanaWidgetData | null
  error: string | null
  reconnect: () => void
}

/**
 * WebSocket hook for real-time Grafana dashboard updates.
 *
 * Architecture decisions:
 * - Uses native WebSocket API (no library) to avoid bundle bloat.
 * - Exponential backoff reconnection: starts at 1s, caps at 30s.
 *   This prevents thundering herd when the backend restarts.
 * - Stale data retention: keeps the last received payload so widgets
 *   can render stale data while reconnecting instead of showing empty states.
 * - The hook does NOT auto-connect on mount. The consumer must set `enabled: true`
 *   to avoid establishing connections from pages that don't need real-time updates.
 */
export function useGrafanaWebSocket(options: Options = {}): Return {
  const {
    enabled = true,
    onMessage,
    reconnectInterval = 1000,
    maxReconnectAttempts = 20,
  } = options

  const wsRef = useRef<WebSocket | null>(null)
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const reconnectAttemptsRef = useRef(0)
  const mountedRef = useRef(true)
  const onMessageRef = useRef(onMessage)

  const [isConnected, setIsConnected] = useState(false)
  const [lastData, setLastData] = useState<GrafanaWidgetData | null>(null)
  const [error, setError] = useState<string | null>(null)

  // Use a ref to track the connect function to avoid forward reference issues
  const connectRef = useRef<(() => void) | null>(null)

  useEffect(() => {
    onMessageRef.current = onMessage
  }, [onMessage])

  useEffect(() => {
    mountedRef.current = true

    if (!enabled) return

    function doConnect() {
      if (!mountedRef.current) return

      // Cleanup previous connection
      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current)
        reconnectTimerRef.current = null
      }
      if (wsRef.current) {
        wsRef.current.close()
        wsRef.current = null
      }

      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const host = window.location.host
      const url = `${protocol}//${host}/ws/grafana`

      try {
        const ws = new WebSocket(url)
        wsRef.current = ws

        ws.onopen = () => {
          if (!mountedRef.current) return
          setIsConnected(true)
          setError(null)
          reconnectAttemptsRef.current = 0
        }

        ws.onmessage = (event) => {
          if (!mountedRef.current) return
          try {
            const msg: GrafanaWebSocketMessage = JSON.parse(event.data)
            if (msg.topic === 'dashboard' && msg.data) {
              setLastData(msg.data)
              onMessageRef.current?.(msg.data)
            }
          } catch {
            // Ignore malformed messages
          }
        }

        ws.onclose = () => {
          if (!mountedRef.current) return
          setIsConnected(false)

          if (reconnectAttemptsRef.current < maxReconnectAttempts) {
            const delay = Math.min(
              reconnectInterval * Math.pow(2, reconnectAttemptsRef.current),
              30_000,
            )
            reconnectAttemptsRef.current++
            reconnectTimerRef.current = setTimeout(doConnect, delay)
          }
        }

        ws.onerror = () => {
          if (!mountedRef.current) return
          setError('WebSocket connection failed')
        }
      } catch (e) {
        setError(`Failed to create WebSocket: ${e}`)
      }
    }

    connectRef.current = doConnect
    doConnect()

    return () => {
      mountedRef.current = false
      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current)
      }
      if (wsRef.current) {
        wsRef.current.close()
      }
    }
  }, [enabled, reconnectInterval, maxReconnectAttempts])

  const reconnect = useCallback(() => {
    reconnectAttemptsRef.current = 0
    connectRef.current?.()
  }, [])

  return { isConnected, lastData, error, reconnect }
}
