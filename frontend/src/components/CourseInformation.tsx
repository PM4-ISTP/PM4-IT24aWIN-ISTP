import { Divider, Group, Paper, Stack, Title, Text, Avatar } from "@mantine/core";
import type { CourseDetailResponseDto, CourseInstructorResponseDto } from "@/src/types/course";
import ReadonlyHtmlField from "@/src/components/ReadonlyHtmlField";
import { getInitials } from "@/src/lib/utils";

const getOwner = (instructors: CourseInstructorResponseDto[]) => {
  const owner = instructors.find((instructor) => instructor.instructorRole === "OWNER");
  if (owner === undefined) {
    throw new Error("Something went wrong while loading this course. A course must have an owner.");
  }
  return owner;
};

const getCollaborators = (instructors: CourseInstructorResponseDto[]) => {
  return instructors.filter((instructor) => instructor.instructorRole === "COLLABORATOR");
};

function UserAvatar({
  pictureUrl,
  userName,
}: {
  pictureUrl: string | null | undefined;
  userName: string;
}) {
  return (
    <Avatar radius="xl" color="blue" src={pictureUrl ?? undefined}>
      {getInitials(userName)}
    </Avatar>
  );
}

export default function CourseInformation({ courseData }: { courseData: CourseDetailResponseDto }) {
  const instructors = courseData.courseInstructors;
  const owner = getOwner(instructors);
  const collaborators = getCollaborators(instructors);

  return (
    <Stack>
      <Title order={1}>{courseData.title}</Title>
      <Paper shadow="xs">
        <ReadonlyHtmlField content={courseData.description} />
      </Paper>
      <Divider />
      <Group>
        <Text size="sm">Created: {new Date(courseData.createdAt).toLocaleString()}</Text>
        <Text size="sm">Updated: {new Date(courseData.updatedAt).toLocaleString()}</Text>
      </Group>
      <Divider />
      <Stack>
        <Title order={3}>Owner</Title>
        <Group>
          <UserAvatar pictureUrl={owner.instructor.picture} userName={owner.instructor.name} />
          <Text>{owner.instructor.name}</Text>
        </Group>
      </Stack>
      <Stack>
        <Title order={3}>Collaborators</Title>
        {collaborators.map((collaborator) => (
          <Group key={collaborator.id}>
            <UserAvatar
              pictureUrl={collaborator.instructor.picture}
              userName={collaborator.instructor.name}
            />
            <Text>{collaborator.instructor.name}</Text>
          </Group>
        ))}
      </Stack>
    </Stack>
  );
}
