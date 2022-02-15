import Handler from '../../rest/handler'

const handler = new Handler('./components/graphql/Demo6ById.graphql', ['id'])
export default async (req, res) => {
  await handler.handle(req, res)
}