import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import { useRunSimulation, usePollingSimulation } from '../hooks/useSimulation'
import { useAnalyzerStore } from '../store'
import type { AnalysisRequestDto } from '../types'

const schema = z.object({
  scenarioName: z
    .string()
    .min(3, 'At least 3 characters')
    .max(100, 'Max 100 characters')
    .regex(/^[a-zA-Z0-9_\- ]+$/, 'Letters, numbers, spaces, hyphens, underscores only'),
  targetService: z.string().min(1, 'Required'),
  durationSeconds: z.number().min(1).max(3600),
  concurrentUsers: z.number().min(1).max(10000),
  errorRateThreshold: z.number().min(0).max(1),
})

type FormValues = z.infer<typeof schema>

export default function SimulationLaunch() {
  const runMutation = useRunSimulation()
  const pollingId = useAnalyzerStore((s) => s.pollingSimulationId)
  const [submittedId, setSubmittedId] = useState<number | null>(null)

  usePollingSimulation(submittedId)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      scenarioName: '',
      targetService: '',
      durationSeconds: 60,
      concurrentUsers: 100,
      errorRateThreshold: 0.05,
    },
  })

  const onSubmit = (data: FormValues) => {
    const dto: AnalysisRequestDto = {
      scenarioName: data.scenarioName,
      targetService: data.targetService,
      durationSeconds: data.durationSeconds,
      concurrentUsers: data.concurrentUsers,
      errorRateThreshold: data.errorRateThreshold,
    }
    runMutation.mutate(dto, {
      onSuccess: (response) => {
        setSubmittedId(response.data.id)
      },
    })
  }

  return (
    <div className="mx-auto max-w-2xl">
      <h2 className="text-lg font-semibold text-gray-900 mb-6">Run a New Simulation</h2>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Scenario Name
          </label>
          <input
            {...register('scenarioName')}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            placeholder="e.g. checkout-load-test"
          />
          {errors.scenarioName && (
            <p className="mt-1 text-xs text-red-600">{errors.scenarioName.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Target Service
          </label>
          <input
            {...register('targetService')}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            placeholder="e.g. order-service"
          />
          {errors.targetService && (
            <p className="mt-1 text-xs text-red-600">{errors.targetService.message}</p>
          )}
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Duration (seconds)
            </label>
            <input
              type="number"
              {...register('durationSeconds')}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            />
            {errors.durationSeconds && (
              <p className="mt-1 text-xs text-red-600">
                {errors.durationSeconds.message}
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Concurrent Users
            </label>
            <input
              type="number"
              {...register('concurrentUsers')}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            />
            {errors.concurrentUsers && (
              <p className="mt-1 text-xs text-red-600">
                {errors.concurrentUsers.message}
              </p>
            )}
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Error Rate Threshold (%)
          </label>
          <input
            type="number"
            step="0.01"
            {...register('errorRateThreshold')}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
          />
          {errors.errorRateThreshold && (
            <p className="mt-1 text-xs text-red-600">
              {errors.errorRateThreshold.message}
            </p>
          )}
        </div>

        <button
          type="submit"
          disabled={runMutation.isPending}
          className="w-full inline-flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {runMutation.isPending ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              Starting...
            </>
          ) : (
            'Launch Simulation'
          )}
        </button>
      </form>

      {submittedId && pollingId === submittedId && (
        <div className="mt-6 rounded-xl border border-blue-200 bg-blue-50 p-4">
          <div className="flex items-center gap-2">
            <Loader2 className="h-4 w-4 animate-spin text-blue-600" />
            <p className="text-sm font-medium text-blue-800">
              Simulation #{submittedId} is running... (checking every 2s)
            </p>
          </div>
        </div>
      )}
    </div>
  )
}
