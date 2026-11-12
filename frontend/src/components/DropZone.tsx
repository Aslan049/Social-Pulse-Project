import { useState } from 'react'
import type { ChangeEvent, DragEvent } from 'react'

interface DropZoneProps {
  file: File | null
  disabled?: boolean
  onFileSelected: (file: File) => void
}

export function DropZone({ file, disabled = false, onFileSelected }: DropZoneProps) {
  const [dragging, setDragging] = useState(false)

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault()
    setDragging(false)
    if (disabled) {
      return
    }
    const dropped = event.dataTransfer.files[0]
    if (dropped) {
      onFileSelected(dropped)
    }
  }

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const selected = event.target.files?.[0]
    if (selected) {
      onFileSelected(selected)
    }
    event.target.value = '' // aynı dosya tekrar seçilebilsin
  }

  return (
    <div
      className={`dropzone${dragging ? ' dragging' : ''}${disabled ? ' disabled' : ''}`}
      onDragOver={(event) => {
        event.preventDefault()
        setDragging(true)
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={handleDrop}
    >
      <p>CSV veya Excel (.xlsx) dosyasını buraya sürükleyin</p>
      <label className="file-button">
        Dosya seç
        <input
          className="visually-hidden"
          type="file"
          accept=".csv,.xlsx"
          disabled={disabled}
          onChange={handleChange}
        />
      </label>
      {file && (
        <p className="selected-file">
          Seçilen dosya: <strong>{file.name}</strong>
        </p>
      )}
    </div>
  )
}
