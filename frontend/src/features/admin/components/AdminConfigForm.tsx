"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import {
  Center,
  Fieldset,
  Grid,
  Group,
  NumberInput,
  Select,
  Stack,
  Text,
  TextInput,
} from "@mantine/core";
import { Dropzone } from "@mantine/dropzone";
import { useForm } from "@mantine/form";
import { IconCloudUpload, IconFile, IconX } from "@tabler/icons-react";
import AppButton from "@/src/shared/components/AppButton";
import { useApiClient } from "@/src/shared/lib/api/client";
import type { components } from "@/src/shared/lib/api/schema";
import {
  memorySpecificationToString,
  MemoryUnit,
  memoryUnits,
  stringToMemorySpecification,
} from "@/src/shared/lib/memoryUnit";
import { useAsyncAction } from "@/src/shared/hooks/useAsyncAction";
import { notifications } from "@mantine/notifications";

type AdminConfigResponse = components["schemas"]["AdminConfigResponse"];

type Props = {
  initialConfig: AdminConfigResponse;
};

export default function AdminConfigForm({ initialConfig }: Props) {
  const router = useRouter();
  const apiClient = useApiClient();

  const config: AdminConfigResponse = initialConfig;

  const defaultMemoryUnit = MemoryUnit.MebiByte;

  const getInitialFormValues = (adminConfig: AdminConfigResponse) => {
    try {
      const memorySpecification =
        adminConfig.memoryLimit == null
          ? null
          : stringToMemorySpecification(adminConfig.memoryLimit);
      const hasMemorySpecification = memorySpecification !== null;
      return {
        cpuLimit: adminConfig.cpuLimit ?? "",
        memoryLimit: hasMemorySpecification ? String(memorySpecification.value) : "",
        memoryLimitUnit: hasMemorySpecification ? memorySpecification.unit : defaultMemoryUnit,
        imagePullSecretName: adminConfig.imagePullSecretName ?? "",
        podTtlSeconds: adminConfig.podTtlSeconds ?? 3600,
        kubeconfig: null as File | null,
      };
    } catch {
      return {
        cpuLimit: "",
        memoryLimit: "",
        memoryLimitUnit: defaultMemoryUnit,
        imagePullSecretName: "",
        podTtlSeconds: 3600,
        kubeconfig: null as File | null,
      };
    }
  };

  const openRef = useRef<() => void>(null!);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const form = useForm({
    mode: "uncontrolled",
    initialValues: getInitialFormValues(config),
    validate: {
      // In Firefox, the required attribute on FileInput does nothing. Therefor, custom validation is needed.
      kubeconfig: (value) => {
        if (!config.kubeconfigUploaded && value === null) {
          return "You need to upload a Kubeconfig file.";
        }
        return null;
      },
    },
  });

  const handleSubmit = async (values: typeof form.values) => {
    let kubeconfigBase64: string | undefined;
    if (values.kubeconfig !== null) {
      const buffer = await values.kubeconfig.arrayBuffer();
      kubeconfigBase64 = btoa(String.fromCharCode(...new Uint8Array(buffer)));
    }

    const cpuLimit = values.cpuLimit !== "" ? String(values.cpuLimit) : undefined;
    const memoryLimit =
      values.memoryLimit !== ""
        ? memorySpecificationToString({
            value: Number.parseInt(String(values.memoryLimit)),
            unit: values.memoryLimitUnit,
          })
        : undefined;
    const imagePullSecretName = values.imagePullSecretName.trim();

    try {
      const rawPodTtlSeconds = values.podTtlSeconds;
      const podTtlSeconds =
        rawPodTtlSeconds != null && String(rawPodTtlSeconds).trim() !== ""
          ? Number(rawPodTtlSeconds)
          : undefined;

      if (!config.kubeconfigUploaded) {
        const { error } = await apiClient.POST("/api/admin/config", {
          body: {
            kubeconfig: kubeconfigBase64,
            cpuLimit,
            memoryLimit,
            imagePullSecretName,
            podTtlSeconds,
          },
        });
        if (error) throw new Error(JSON.stringify(error));
      } else {
        const { error } = await apiClient.PUT("/api/admin/config", {
          body: {
            kubeconfig: kubeconfigBase64,
            cpuLimit,
            memoryLimit,
            imagePullSecretName,
            podTtlSeconds,
          },
        });
        if (error) throw new Error(JSON.stringify(error));
      }
      notifications.show({
        title: "Success",
        message: "Admin configuration has been successfully updated.",
        color: "green",
      });
      router.refresh();
    } catch (e) {
      notifications.show({
        title: "Error",
        message: "It was not possible to submit the admin configuration: " + (e as Error).message,
        color: "red",
      });
    }
  };

  const deleteAction = useAsyncAction(
    async () => {
      const { error } = await apiClient.DELETE("/api/admin/config");
      if (error) throw new Error(JSON.stringify(error));
    },
    {
      id: "admin-config-delete",
      successTitle: "Success",
      successMessage: "Admin configuration has been successfully deleted.",
      errorTitle: "Error",
      errorMessage: (e) => `It was not possible to delete the admin configuration: ${e.message}`,
      onSuccess: () => router.refresh(),
    }
  );

  return (
    <form onSubmit={form.onSubmit((values) => void handleSubmit(values))}>
      <Text fw={600} size="lg">
        Kubernetes Configuration
      </Text>
      <Text c="dimmed" size="sm" mt={4}>
        Required fields are marked with *
      </Text>

      <Fieldset id="admin-config-form">
        <Grid>
          <Grid.Col span={12}>
            <NumberInput
              id="cpu-limit-input"
              label="CPU limit"
              description="How much CPU one single pod can at maximum use."
              key={form.key("cpuLimit")}
              {...form.getInputProps("cpuLimit")}
              min={1}
              allowNegative={false}
              allowDecimal={false}
              clampBehavior="strict"
              required={config.cpuLimit != null}
              withAsterisk={config.cpuLimit != null}
            />
          </Grid.Col>
          <Grid.Col span={12}>
            <Grid align="flex-end">
              <Grid.Col span={8}>
                <NumberInput
                  id="memory-limit-input"
                  label="Memory limit"
                  description="How much memory one single pod can at maximum use."
                  key={form.key("memoryLimit")}
                  {...form.getInputProps("memoryLimit")}
                  min={1}
                  allowNegative={false}
                  allowDecimal={false}
                  clampBehavior="strict"
                  required={config.memoryLimit != null}
                  withAsterisk={config.memoryLimit != null}
                />
              </Grid.Col>
              <Grid.Col span={4}>
                <Select
                  id="memory-unit-input"
                  label="Memory unit"
                  description="Unit for the memory limit above."
                  key={form.key("memoryLimitUnit")}
                  {...form.getInputProps("memoryLimitUnit")}
                  data={memoryUnits}
                  allowDeselect={false}
                  required={config.memoryLimit != null}
                  withAsterisk={config.memoryLimit != null}
                />
              </Grid.Col>
            </Grid>
          </Grid.Col>
          <Grid.Col span={12}>
            <TextInput
              id="image-pull-secret-input"
              label="Image pull secret"
              description="Optional Kubernetes secret name for private GHCR lab images."
              placeholder="ghcr-pull-secret"
              key={form.key("imagePullSecretName")}
              {...form.getInputProps("imagePullSecretName")}
            />
          </Grid.Col>
          <Grid.Col span={12}>
            <NumberInput
              id="pod-ttl-input"
              label="Pod TTL (seconds)"
              description="How long a lab pod stays alive before it is automatically cleaned up."
              key={form.key("podTtlSeconds")}
              {...form.getInputProps("podTtlSeconds")}
              min={60}
              max={86400}
              step={60}
              allowNegative={false}
              allowDecimal={false}
              clampBehavior="strict"
            />
          </Grid.Col>
          <Grid.Col span={12}>
            <Stack gap={4}>
              <Text size="sm" fw={500}>
                Kubeconfig
                {!config.kubeconfigUploaded && (
                  <Text component="span" c="red" ml={2}>
                    *
                  </Text>
                )}
              </Text>
              <Text size="xs" c="dimmed">
                Upload your Kubeconfig file for the Kubernetes cluster that manages the lab pods.
              </Text>
              <Dropzone
                id="kubeconfig-input"
                aria-label="Kubeconfig"
                openRef={openRef}
                onDrop={(files) => {
                  const file = files[0] ?? null;
                  form.setFieldValue("kubeconfig", file);
                  form.clearFieldError("kubeconfig");
                  setSelectedFile(file);
                }}
                maxFiles={1}
                disabled={form.submitting}
                style={{
                  border: "2px dashed",
                  borderColor: form.errors.kubeconfig
                    ? "var(--mantine-color-red-6)"
                    : "rgba(255,255,255,0.15)",
                  borderRadius: 10,
                  background: "rgba(255,255,255,0.03)",
                  cursor: "pointer",
                  transition: "border-color 150ms ease, background 150ms ease",
                }}
              >
                <Center py="md">
                  <Dropzone.Accept>
                    <IconCloudUpload size={40} color="#2563eb" />
                  </Dropzone.Accept>
                  <Dropzone.Reject>
                    <IconX size={40} color="var(--mantine-color-red-6)" />
                  </Dropzone.Reject>
                  <Dropzone.Idle>
                    {selectedFile ? (
                      <Stack align="center" gap={6}>
                        <IconFile size={40} color="#94a3b8" />
                        <Text size="sm" c="dimmed">
                          {selectedFile.name}
                        </Text>
                        <Text size="xs" c="dimmed">
                          Click to replace
                        </Text>
                      </Stack>
                    ) : (
                      <Stack align="center" gap={6}>
                        <IconCloudUpload size={40} color="#94a3b8" />
                        <Text size="sm" fw={500}>
                          Drag &amp; drop your kubeconfig here
                        </Text>
                        <Text size="xs" c="dimmed">
                          or
                        </Text>
                        <AppButton
                          size="xs"
                          aria-label="Kubeconfig"
                          onClick={(e) => {
                            e.stopPropagation();
                            openRef.current?.();
                          }}
                        >
                          Browse file
                        </AppButton>
                      </Stack>
                    )}
                  </Dropzone.Idle>
                </Center>
              </Dropzone>
              {form.errors.kubeconfig && (
                <Text size="xs" c="red">
                  {form.errors.kubeconfig}
                </Text>
              )}
              {config.kubeconfigUploaded && !selectedFile && (
                <Text size="xs" c="dimmed">
                  A kubeconfig is already uploaded. Upload a new file only if you want to replace
                  it.
                </Text>
              )}
            </Stack>
          </Grid.Col>
        </Grid>

        <Group justify="space-between" mt="md">
          <AppButton id="admin-config-form-submit-button" type="submit" loading={form.submitting}>
            {!config.kubeconfigUploaded
              ? "Create Kubernetes configuration"
              : "Update Kubernetes configuration"}
          </AppButton>
          <AppButton
            tone="danger"
            id="admin-config-form-delete-button"
            type="button"
            onClick={() => void deleteAction.run()}
            loading={form.submitting || deleteAction.loading}
          >
            Delete Kubernetes configuration
          </AppButton>
        </Group>
      </Fieldset>
    </form>
  );
}
