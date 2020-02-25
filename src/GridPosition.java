/* basically a 2-tuple to hold the position in the grid */
class GridPosition
{
    public final int row;  public final int column;

    @Override
    public boolean equals(Object obj)
    {
        try
        {
            GridPosition o = (GridPosition) obj;
            return (o.row == this.row && o.column == this.column);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public GridPosition(int row, int column)
    {
        this.row = row;  this.column = column;
    }
}
