import { useEffect, useRef } from 'react'

type Options = {
  enabled: boolean
  onMessage: (data: unknown) => void
}

function wsUrl(): string {
  const fromEnv = (import.meta.env.VITE_WS_URL as string | undefined)?.trim()
  if (fromEnv) return fromEnv

  const { protocol, hostname } = window.location
  const isHttps = protocol === 'https:'
  const wsProto = isHttps ? 'wss' : 'ws'

  const port = window.location.port === '5173' ? '8080' : window.location.port
  return `${wsProto}://${hostname}:${port}/rawws`
}

export function useRawWebSocket({ enabled, onMessage }: Options) {
  const onMessageRef = useRef(onMessage)
  onMessageRef.current = onMessage

  useEffect(() => {
    if (!enabled) return

    let socket: WebSocket | null = null
    let closed = false
    let retryTimer: number | null = null

    const connect = () => {
      if (closed) return
      try {
        socket = new WebSocket(wsUrl())
      } catch {
        retryTimer = window.setTimeout(connect, 1500)
        return
      }

      socket.onmessage = (evt) => {
        const text = String(evt.data ?? '')
        try {
          onMessageRef.current(JSON.parse(text))
        } catch {
          onMessageRef.current(text)
        }
      }

      socket.onclose = () => {
        if (closed) return
        retryTimer = window.setTimeout(connect, 1500)
      }

      socket.onerror = () => {
      }
    }

    connect()

    return () => {
      closed = true
      if (retryTimer) window.clearTimeout(retryTimer)
      try {
        socket?.close()
      } catch {
      }
    }
  }, [enabled])
}
